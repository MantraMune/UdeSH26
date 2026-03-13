
#include "ParticuleMaterial.h"
#include "Node.h"
#include <glm/gtc/type_ptr.hpp>
#include <glm/gtc/random.hpp>

ParticuleMaterial::ParticuleMaterial(std::string name) :
	MaterialGL(name)
{

	vp = new GLProgram(MaterialPath + "ParticuleMaterial/ParticuleMaterial-VS.glsl", GL_VERTEX_SHADER);
	fp = new GLProgram(MaterialPath + "ParticuleMaterial/ParticuleMaterial-FS.glsl", GL_FRAGMENT_SHADER);
    

	m_ProgramPipeline->useProgramStage(vp, GL_VERTEX_SHADER_BIT);
	m_ProgramPipeline->useProgramStage(fp, GL_FRAGMENT_SHADER_BIT);


	l_ViewProj = glGetUniformLocation(vp->getId(), "ViewProj");
	l_Model = glGetUniformLocation(vp->getId(), "Model");
	l_PosLum = glGetUniformLocation(vp->getId(), "PosLum");
	l_PosCam = glGetUniformLocation(vp->getId(), "PosCam");
    l_Time = glGetUniformLocation(vp->getId(), "Time");



	l_Phong = glGetUniformLocation(fp->getId(), "Phong");
	l_Albedo = glGetUniformLocation(fp->getId(), "diffuseAlbedo");
	l_specColor = glGetUniformLocation(fp->getId(), "specularColor");


	param.albedo = glm::vec3(0.2, 0.7, 0.8);
	param.coeff = glm::vec4(0.2,0.8,1.0,100.0);
	param.specularColor = glm::vec3(1.0);

	glCreateQueries(GL_TIME_ELAPSED, 1, &mQueryTimeElapsed);
    mSimTime = 0;
    m_GridDim = glm::ivec3(20, 20, 20);
    m_HashTableSize = m_GridDim.x * m_GridDim.y * m_GridDim.z;
	
	// Créer les buffers pour les particules (position, vélocité et couleur (debug)) - Ce sont les buffers essentiels à l'animation des particules
    glCreateBuffers(2, m_Positions); // 2 buffers pour les positions (ping pong - source et cible)
    glCreateBuffers(2, m_Velocities);
	glCreateBuffers(1, &m_Colors);
    glCreateBuffers(1, &m_HashTable);

	// Définir la taille mémoire des buffers à chaque index (surtout pour la position et la vitesse) - Chaque particule a une position (x, y, z) et une info w, pareil pour la vitesse. 
    glNamedBufferStorage(m_Positions[0], PARTICULENUMBER * sizeof(glm::vec4), NULL, GL_DYNAMIC_STORAGE_BIT); // Source
    glNamedBufferStorage(m_Positions[1], PARTICULENUMBER * sizeof(glm::vec4), NULL, GL_DYNAMIC_STORAGE_BIT); // Cible
    glNamedBufferStorage(m_Velocities[0], PARTICULENUMBER * sizeof(glm::vec4), NULL, GL_DYNAMIC_STORAGE_BIT);
    glNamedBufferStorage(m_Velocities[1], PARTICULENUMBER * sizeof(glm::vec4), NULL, GL_DYNAMIC_STORAGE_BIT);
    glNamedBufferStorage(m_Colors, PARTICULENUMBER * sizeof(glm::vec4), NULL, GL_DYNAMIC_STORAGE_BIT);
    glNamedBufferStorage(m_HashTable, m_HashTableSize * sizeof(int), nullptr, GL_DYNAMIC_STORAGE_BIT);
   
	// Répartir les particules aléatoirement - Il faut que les particules soit réparties aléatoirement dans la boîte de collision, donc dans l'intervalle [-9.7, 9.7].
	
	// Positions initales des particules 
    std::vector<glm::vec4> initialPositions(PARTICULENUMBER);

    // Vitesses initiales des particules
	std::vector<glm::vec4> initialVelocities(PARTICULENUMBER, glm::vec4(0.0f));

	for (int i = 0; i < PARTICULENUMBER; i++) {
        glm::vec3 pos = glm::sphericalRand(9.0f);
        initialPositions[i] = glm::vec4(pos, 1.0f); // Pour l'instant, w est la vie d'une particule (0 - 1)
	}

	// Mise à jour du buffer source des positions
	glNamedBufferSubData(m_Positions[0], 0, PARTICULENUMBER * sizeof(glm::vec4), initialPositions.data());
    glNamedBufferSubData(m_Positions[1], 0, PARTICULENUMBER * sizeof(glm::vec4), initialPositions.data());
    glNamedBufferSubData(m_Velocities[0], 0, PARTICULENUMBER * sizeof(glm::vec4), initialVelocities.data());
    glNamedBufferSubData(m_Velocities[1], 0, PARTICULENUMBER * sizeof(glm::vec4), initialVelocities.data());

    // Initialisation de la couleur
    std::vector<glm::vec4> initialColors(PARTICULENUMBER, glm::vec4(1.0f, 0.0f, 0.0f, 1.0f)); // rouge
    glNamedBufferSubData(m_Colors, 0, PARTICULENUMBER * sizeof(glm::vec4), initialColors.data());
	
	// Créer le compute shader
    cp = new GLProgram(MaterialPath + "ParticuleMaterial/ParticuleMaterial-CS.glsl", GL_COMPUTE_SHADER); 
    m_ProgramPipeline->useProgramStage(cp, GL_COMPUTE_SHADER_BIT);                                       
    lDeltaTime = glGetUniformLocation(cp->getId(), "DeltaTime");
    l_GravityDir = glGetUniformLocation(cp->getId(), "GravityDir");
    l_Radius = glGetUniformLocation(cp->getId(), "Radius");
    l_BoxHalfSize = glGetUniformLocation(cp->getId(), "BoxHalfSize");
    l_CellSize = glGetUniformLocation(cp->getId(), "CellSize");
    l_GridDim = glGetUniformLocation(cp->getId(), "GridDim");

    

	physik.mass = 0.1f;
    physik.deltaTime = 0.01f;

	updateSimulationParameters();
	updatePhong();
}

ParticuleMaterial::~ParticuleMaterial()
{

}

void ParticuleMaterial::render(Node* o)
{
	// Lier les buffers position et couleurs (définies en SSBO dans le vertex shader) pour le rendu
    glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, m_Positions[0]); // Position source
    glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, m_Colors);       // Couleur (debug)


	m_ProgramPipeline->bind();

	// Afficher en utilisant l'instanciation - Affiche N modeles
	o->drawGeometryInstanced(GL_TRIANGLES,PARTICULENUMBER);

	m_ProgramPipeline->release();
}
void ParticuleMaterial::animate(Node* o, const float elapsedTime)
{

	glm::mat4 viewproj = Scene::getInstance()->camera()->getProjectionMatrix() * Scene::getInstance()->camera()->getViewMatrix();

	glProgramUniformMatrix4fv(vp->getId(), l_ViewProj, 1, GL_FALSE, glm::value_ptr(viewproj));
	glProgramUniformMatrix4fv(vp->getId(), l_Model, 1, GL_FALSE, glm::value_ptr(o->frame()->getModelMatrix()));
	glProgramUniform3fv(vp->getId(), l_PosLum, 1,  glm::value_ptr(Scene::getInstance()->getNode("Light")->frame()->convertPtTo(glm::vec3(0.0,0.0,0.0),o->frame())));
	glProgramUniform3fv(vp->getId(), l_PosCam, 1, glm::value_ptr(Scene::getInstance()->camera()->frame()->convertPtTo(glm::vec3(0.0, 0.0, 0.0), o->frame())));
    glProgramUniform1d(vp->getId(), l_Time, elapsedTime);

	auto now_time = std::chrono::high_resolution_clock::now();
	auto timevar = now_time.time_since_epoch();
	float millis = 0.001f*std::chrono::duration_cast<std::chrono::milliseconds>(timevar).count();
	
	/*Direction du vecteur up dans le rep�re de l'objet. A utiliser pour d�finir la direction de la force de gravit�*/
	glm::vec3 gravityDir = Scene::getInstance()->getSceneNode()->frame()->convertDirTo(glm::vec3(0.0, -1.0, 0.0), o->frame());
    
	// Envoi de la valeur de la gravité et de DeltaTime au compute shader
    glProgramUniform3fv(cp->getId(), l_GravityDir, 1, glm::value_ptr(gravityDir));
    glProgramUniform1f(cp->getId(), lDeltaTime, physik.deltaTime);
    glProgramUniform1f(cp->getId(), l_Radius, 0.3f);
    glProgramUniform1f(cp->getId(), l_BoxHalfSize, 10.0f);
    glProgramUniform1f(cp->getId(), l_CellSize, 1.0f);
    glProgramUniform3iv(cp->getId(), l_GridDim, 1, glm::value_ptr(m_GridDim));

    // Vidage de la table
    std::vector<int> empty(m_HashTableSize, -1);
    glNamedBufferSubData(m_HashTable, 0, m_HashTableSize * sizeof(int), empty.data());

	simulation();
}

void ParticuleMaterial::simulation()
{

	glBeginQuery(GL_TIME_ELAPSED, mQueryTimeElapsed);

	m_ProgramPipeline->bind();

    // Lier les buffers positions (source et cible) et vitesse (source et cible) pour le compute shader
    glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, m_Positions[0]);  // Position source (lecture)
    glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, m_Positions[1]);  // Position cible (écriture)
    glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 2, m_Velocities[0]); // Vélocité source (lecture)
    glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 3, m_Velocities[1]); // Vélocité cible (écriture)
    glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 4, m_HashTable);	// Table de hachage (collisions)

	// Lancer le compute shader (dispatchCompute)
    GLuint workGroupSize = 256; // Nombre de threads par groupe de travail
    GLuint numGroups = (PARTICULENUMBER + workGroupSize - 1) / workGroupSize; // Calcul du nombre de groupes nécessaires
    glDispatchCompute(numGroups, 1, 1);                                       // Lancer le compute shader

    // Échanger les buffers source/cible pour le prochain frame avant le rendu
    std::swap(m_Positions[0], m_Positions[1]);
    std::swap(m_Velocities[0], m_Velocities[1]);

	// Synchroniser avant le rendu
    glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);

    glUseProgram(NULL);
    glEndQuery(GL_TIME_ELAPSED);
    GLuint64 result = static_cast<GLuint64>(0);
    glGetQueryObjectui64v(mQueryTimeElapsed, GL_QUERY_RESULT, &result);
    mSimTime = result;

	

}


void ParticuleMaterial::updatePhong()
{
	glProgramUniform4fv(fp->getId(), l_Phong, 1, glm::value_ptr(glm::vec4(param.coeff)));
	glProgramUniform3fv(fp->getId(), l_Albedo, 1, glm::value_ptr(param.albedo));
	glProgramUniform3fv(fp->getId(), l_specColor, 1, glm::value_ptr(param.specularColor));
}



void ParticuleMaterial::updateSimulationParameters()
{
    // Étape 1 : Définir les limites de la boîte de collision
    const float boxLimit = 10.0f;
    const float radius = 0.3f; // Rayon des sphères

    // Étape 2 : Lire les positions et vitesses du buffer source
    std::vector<glm::vec4> positions(PARTICULENUMBER);
    std::vector<glm::vec4> velocities(PARTICULENUMBER);

    glGetNamedBufferSubData(m_Positions[0], 0, PARTICULENUMBER * sizeof(glm::vec4), positions.data());
    glGetNamedBufferSubData(m_Velocities[0], 0, PARTICULENUMBER * sizeof(glm::vec4), velocities.data());

    // Étape 3 : Collisions (bordures de la boîte)
    for (int i = 0; i < PARTICULENUMBER; ++i) {
        for (int axis = 0; axis < 3; ++axis) // Pour x, y, z
        {
            if (positions[i][axis] - radius < -boxLimit) // Collision avec la face "négative" (face gauche, bas ou arrière)
            {
                positions[i][axis] = -boxLimit + radius; // Repositionner juste après la limite
                velocities[i][axis] *= -1.0f;            // Inverser la composante de la vitesse (rebond)
            }
            if (positions[i][axis] + radius > boxLimit) // Collision avec la face "positive" (face droite, haut ou avant)
            {
                positions[i][axis] = boxLimit - radius; // Repositionner juste avant la limite
                velocities[i][axis] *= -1.0f;           // Inverser la composante de la vitesse (rebond)
            }
        }
    }

	// Étape 4 : Collisions (particules) -> O(n^2) - Paires de particules (i, j) - Optimisé avec une table de hachage
	for (int i = 0; i < PARTICULENUMBER; ++i) // Particule 1
	{
		for (int j = i + 1; j < PARTICULENUMBER; ++j)  // Particule 2
		{
            glm::vec3 delta = glm::vec3(positions[i]) - glm::vec3(positions[j]); 
            float dist = glm::length(delta);
			if (dist < 2.0f * radius && dist > 0.0f) // Si la distance entre deux particules est plus petite que le diamètre d'une particule et positif
			{											// Cas de chevauchement vérifié
				// Calcul direction de la collision
                glm::vec3 dir = glm::normalize(delta);

				// Vitesse relative le long de la direction
                float vRel = glm::dot((glm::vec3(velocities[i]) - glm::vec3(velocities[j])), dir); // vRel > 0 (éloignement), vRel < 0 (rapprochement)

				// Échange simple de la vitesse le long de la direction
                glm::vec3 vChange = 0.5f * vRel * dir;
                velocities[i].x -= vChange.x;
                velocities[i].y -= vChange.y;
                velocities[i].z -= vChange.z;

                velocities[j].x += vChange.x;
                velocities[j].y += vChange.y;
                velocities[j].z += vChange.z;

				// Séparer les particules (anti-chevauchement)
                float penetration = 2.0f * radius - dist; // Distance de chevauchement
                positions[i].x += dir.x * 0.5f * penetration;
                positions[i].y += dir.y * 0.5f * penetration;
                positions[i].z += dir.z * 0.5f * penetration;

                positions[j].x -= dir.x * 0.5f * penetration;
                positions[j].y -= dir.y * 0.5f * penetration;
                positions[j].z -= dir.z * 0.5f * penetration;
			}
		}
	}

	// Étape 5 : Réécrire dans les buffers cible
    glNamedBufferSubData(m_Positions[1], 0, PARTICULENUMBER * sizeof(glm::vec4), positions.data());
    glNamedBufferSubData(m_Velocities[1], 0, PARTICULENUMBER * sizeof(glm::vec4), velocities.data());
}




void ParticuleMaterial::displayInterface()
{

	if (ImGui::TreeNode("Physical parameters"))
	{
        ImGui::BeginGroup();

        ImGui::Text("Simulaion time : %f ms/frame", (mSimTime * 1.e-6));
			
			bool upd = false;
            upd = ImGui::SliderFloat("Particule", &physik.mass, 0.1f, 10.0f, "Mass = %.3f") || upd;
            upd = ImGui::SliderFloat("deltaTime", &physik.deltaTime, 0.0f, 0.2f, "DeltaTime = %.3f") || upd;
			
			ImGui::EndGroup();
			ImGui::Separator();
			ImGui::Spacing();

			ImGui::TreePop();

			if (upd)
                updateSimulationParameters();
	}
	if (ImGui::TreeNode("PhongParameters"))
	{
	bool upd = false;
		upd = ImGui::SliderFloat("ambiant", &param.coeff.x, 0.0f, 1.0f, "ambiant = %.2f") || upd;
		upd = ImGui::SliderFloat("diffuse", &param.coeff.y, 0.0f, 1.0f, "diffuse = %.2f") || upd;
		upd = ImGui::SliderFloat("specular", &param.coeff.z, 0.0f, 2.0f, "specular = %.2f") || upd;
		upd = ImGui::SliderFloat("exposant", &param.coeff.w, 0.1f, 200.0f, "exposant = %f") || upd;
		ImGui::PushItemWidth(200.0f);
		upd = ImGui::ColorPicker3("Albedo", glm::value_ptr(param.albedo)) || upd;;
		ImGui::SameLine();
		upd = ImGui::ColorPicker3("Specular Color", glm::value_ptr(param.specularColor)) || upd;;
		ImGui::PopItemWidth();
		if (upd)
			updatePhong();
		ImGui::TreePop();
	}


}




