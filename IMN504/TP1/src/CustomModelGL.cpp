#include "CustomModelGL.h"
#include <iostream>


CustomModelGL::CustomModelGL(std::string name, int _nbElements){
    this->m_Name = name;

    m_nbElements = _nbElements;

    m_Model = new GeometricModel();

    createDeformableGrid();
}


void CustomModelGL::createDeformableGrid()

{
    for (int i = 0; i < m_nbElements; i++){
        for (int j = 0; j < m_nbElements; j++) {
            glm::vec3 pos(j, i, 0.0f);
            m_Model->listVertex.push_back(pos);
            m_Model->listCoords.push_back(glm::vec3((float)j / (float)(m_nbElements - 1),
        (float)i / (float)(m_nbElements - 1),
        0));
            V.push_back(glm::vec3(0.0f));
            F.push_back(glm::vec3(0.0f));
        }
    }
			
   for (int i = 0; i < m_nbElements - 1; i++) {
        for (int j = 0; j < m_nbElements - 1; j++) {

            int base = indice(i, j);

            Face f1{base, base + 1, base + m_nbElements + 1};
            Face f2{base, base + m_nbElements + 1, base + m_nbElements};

            m_Model->listFaces.push_back(f1);
            m_Model->listFaces.push_back(f2);
            m_Model->listCoordFaces.push_back(f1);
            m_Model->listCoordFaces.push_back(f2);
        
        }
    }
			
	// Taille et centre
    glm::vec3 minv(FLT_MAX);  // Initialiser le vecteur 3D de minimum avec la valeur maximale possible
    glm::vec3 maxv(-FLT_MAX); // Initialiser le vecteur 3D de maximum avec la valeur minimale possible

	for (auto &v : m_Model->listVertex)
	{
        minv = glm::min(minv, v); // Prendre le minimum entre le minimum actuel et le sommet v
        maxv = glm::max(maxv, v); // Prendre le maximum entre le maximum actuel et le sommet v
	}

	glm::vec3 mean = (minv + maxv) * 0.5f;
	
	glm::vec3 size = maxv - minv; // Calculer la taille du modèle en soustrayant le minimum du maximum
    float factor = std::max(size.x, size.y);

    for (auto &v : m_Model->listVertex)
	{
		v -= mean;
        if (factor > 1e-6f) v /= factor;
	}

    restPositions = m_Model->listVertex;

	// Mise à jour
	 m_Model->nb_vertex = (int)m_Model->listVertex.size();
	 m_Model->nb_faces = (int)m_Model->listFaces.size();
	 m_Model->loader->computeNormalAndTangents(m_Model);

     // Ressorts

	 auto index = [&](int i, int j) {
         return indice(i, j);
     }; // On crée les index des ressorts selon les indices i et j de la grille.

	 for (int j = 0; j < m_nbElements; j++)
	 {
		 for (int i = 0; i < m_nbElements; i++)
		 {
             int id = index(i, j);

			 // Voisins directs
			 if (i < m_nbElements - 1) // Axe y de la grille 2D
			 {
                 int id2 = index(i + 1, j); // Réfinir les index
                 Spring s; // Crée un objet Ressort (structural)
                 s.id1 = id; // Indice sommet 1
                 s.id2 = id2; // Indice sommet 2
                 s.length = glm::length(m_Model->listVertex[id] - m_Model->listVertex[id2]); // Distance euclidienne entre les deux sommets
                 s.KsFactor = 1.0f;
                 springs.push_back(s); // Ajouté à une liste de ressorts
			 }

			 if (j < m_nbElements - 1) // Axe x de la grille 2D
			 {
                 int id2 = index(i, j + 1);
                 Spring s;
                 s.id1 = id;
                 s.id2 = id2;
                 s.length = glm::length(m_Model->listVertex[id] - m_Model->listVertex[id2]);
                 s.KsFactor = 1.0f;
                 springs.push_back(s);
			 }

			 // Diagonaux
             if (i < m_nbElements - 1 && j < m_nbElements - 1) // Logique, car une case diagonale
             {                                                 // est voisin direct de x puis de y
                 int id2 = index(i + 1, j + 1); 
                 Spring s;// Ressort diagonal (shear)
                 s.id1 = id;
                 s.id2 = id2;
                 s.length = glm::length(m_Model->listVertex[id] - m_Model->listVertex[id2]);
                 s.KsFactor = 0.75f; // Pas nécessairement 1, car on n'a pas un ressort droit (en ligne droite)
                 springs.push_back(s);

				 int id3 = index(i + 1, j); // Sommet en bas
                 int id4 = index(i, j + 1); // Sommet à droite
                 Spring s2; // Ressort structural
                 s2.id1 = id3;
                 s2.id2 = id4;
                 s2.length = glm::length(m_Model->listVertex[id3] - m_Model->listVertex[id4]);
                 s2.KsFactor = 0.75f;
                 springs.push_back(s2);
             }

			 // Distance de 2
			 if (i < m_nbElements - 2)
			 {
                 int id2 = index(i + 2, j);
                 Spring s;
                 s.id1 = id;
                 s.id2 = id2;
                 s.length = glm::length(m_Model->listVertex[id] - m_Model->listVertex[id2]);
                 s.KsFactor = 0.5f; // Ressort plus long donc plus flexible
                 springs.push_back(s);
			 }

			 if (j < m_nbElements - 2)
			 {
                 int id2 = index(i, j + 2);
                 Spring s;
                 s.id1 = id;
                 s.id2 = id2;
                 s.length = glm::length(m_Model->listVertex[id] - m_Model->listVertex[id2]);
                 s.KsFactor = 0.5f;
                 springs.push_back(s);
			 }

		 }
	 }
	
	 loadToGPU();
}

int CustomModelGL::indice(int i, int j) const
{
	return i * m_nbElements + j;
}

void CustomModelGL::recomputeNormals()
{
	m_Model->listNormals.clear();
	m_Model->loader->computeNormals(m_Model);
}



void CustomModelGL::updatePositions()
{
	glNamedBufferData(VBO_Vertex, m_Model->nb_vertex * sizeof(glm::vec3), &(m_Model->listVertex.front()), GL_DYNAMIC_DRAW);
	glNamedBufferData(VBO_Normals, m_Model->nb_vertex * sizeof(glm::vec3), &(m_Model->listNormals.front()), GL_DYNAMIC_DRAW);
}