

#pragma once


#include "MaterialGL.h"

#include "CustomModelGL.h"
#include "Texture2D.h"



struct MSParam
{
	float ks_Stiffness;
	float kd_dampening;
	float mass;
	float deltaTime;
	float wind;
	float windFriction;
};


class MassSpringMaterial : public MaterialGL
{
	struct Phong {
		glm::vec4 coeff;
		glm::vec3 albedo;
		glm::vec3 specularColor;
	};
public:

	//Attributs

	//Constructeur-Destructeur

	/**
		Constructeur de la classe à partir du nom du matériau
		@param name : nom du matériau
	*/
	MassSpringMaterial(string name,Texture2D *t);

	/**
		Destructeur de la classe
	*/
	~MassSpringMaterial();

	//Méthodes

	/**
		Méthode virtuelle qui est appelée pour faire le rendu d'un objet en utilisant ce matériau
		@param o : Node/Objet pour lequel on veut effectuer le rendu
	*/
	virtual void render(Node* o);

	/**
		Méthode virtuelle qui est appelée pour modifier une valeur d'un paramètre nécessaire pour le rendu
		@param o : Node/Objet concerné par le rendu
		@param elapsedTime : temps
	*/
	virtual void animate(Node* o, const float elapsedTime);



	 void updatePhong();

	virtual void displayInterface() ;



	/**
	* Méthode va gérer la simulation au niveau global. Cette méthode est responsable de calculer toutes les forces a appliquer sur chaque sommet et de mettre a jour la simulation en appelant updateSimulation.
	* Les forces a ajouter sont :
	*	La gravité
	*	L'amortissement
	*	Le vent et sa friction
	*	Les ressorts entre chaque sommet
		@param m : le ModelGL custom a mettre a jour
	*/

	void computeMassSpringAnimation(CustomModelGL* m);


	/**
* Cette méthode itère sur tous les ressorts du modèle et appele pour chaque la fonction computeSpringForce
	@param m : le ModelGL custom a mettre a jour
*/
	void computeAllSpringForces(CustomModelGL* m);



	//void computeSpringForce(CustomModelGL* m, int i, int j, int n_i, int n_j,float kFactor = 1.0f);

	/**
	* Cette méthode doit calculer le comportement du ressort s. Elle doit notamment calculer pour les sommets id1 et id2 a chaque bout du ressort la force appliquée par le ressort. Cette fonction doit remplir le tableau F du modèle pour les éléments id1 et id2.
	* La structure du ressort s est la suivante :
	* {
	*		int id1;	// Indice du sommet 1
	*		int id2;	// Indice du sommet 2
	*		float length; // longueur au repos
	*		float KsFactor; // modulation du facteur de rigidité global. Ce facteur va "moduler" le facteur de rigidité globale passé en paramètre. Pour chaque ressort s, le facteur de rigidité total est KsFactor * physik.ks_Stiffness
	*	}
	@param m : le ModelGL custom a mettre a jour
	@param s : le resort en question
	*/

	void computeSpringForce(CustomModelGL* m,Spring s);


	/**
	* Méthode appelée pour mettre a jour la simulation de l'objet. Cette métohde repose sur l'intégration semi-implicite d'Euler.
	* Cette méthode doit calculer et mettre a jour la vitesse (tableau V) ainsi que la position (tableau listVertex de l'objet geometricModel) de chacun des éléments de chacun des éléments du modèle m.
		@param m : le ModelGL custom a mettre a jour
	*/

	void updateSimulation(CustomModelGL* m);

protected:
	GLProgram* vp;
	GLProgram* fp;


	glm::vec3 up_direction, wind_direction;

	GLuint l_ViewProj, l_Model, l_PosLum, l_PosCam, l_Phong, l_Albedo, l_specColor, l_Time, l_tex;
	Phong param;
	MSParam physik;


};
