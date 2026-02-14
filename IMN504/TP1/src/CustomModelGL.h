#pragma once
#include "ModelGL.h"

struct Spring
{
	int id1;	// Indice du sommet 1
	int id2;	// Indice du sommet 2
	float length; // longueur au repos
	float KsFactor; // facteur de rigidit�
};

class CustomModelGL : public ModelGL
{






public:
	CustomModelGL(std::string name, int _nbElements);

	void createDeformableGrid();
	int m_nbElements;
	std::vector<glm::vec3> V;
	std::vector<glm::vec3> F;
    std::vector<glm::vec3> restPositions;

	std::vector<Spring> springs;

	int indice(int i, int j) const;
	void updatePositions();

	void recomputeNormals();
protected:
	

};

