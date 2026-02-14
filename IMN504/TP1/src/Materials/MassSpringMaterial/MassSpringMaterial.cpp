
#include "MassSpringMaterial.h"
#include "Node.h"
#include <glm/gtc/type_ptr.hpp>


MassSpringMaterial::MassSpringMaterial(std::string name,Texture2D *t) :
	MaterialGL(name)
{

	vp = new GLProgram(MaterialPath + "MassSpringMaterial/MassSpringMaterial-VS.glsl", GL_VERTEX_SHADER);
	fp = new GLProgram(MaterialPath + "MassSpringMaterial/MassSpringMaterial-FS.glsl", GL_FRAGMENT_SHADER);

	m_ProgramPipeline->useProgramStage(vp, GL_VERTEX_SHADER_BIT);
	m_ProgramPipeline->useProgramStage(fp, GL_FRAGMENT_SHADER_BIT);


	l_ViewProj = glGetUniformLocation(vp->getId(), "ViewProj");
	l_Model = glGetUniformLocation(vp->getId(), "Model");
	l_PosLum = glGetUniformLocation(vp->getId(), "PosLum");
	l_PosCam = glGetUniformLocation(vp->getId(), "PosCam");



	l_Phong = glGetUniformLocation(fp->getId(), "Phong");
	l_Albedo = glGetUniformLocation(fp->getId(), "diffuseAlbedo");
	l_specColor = glGetUniformLocation(fp->getId(), "specularColor");

	l_tex = glGetUniformLocation(fp->getId(), "ColorTex");
	glProgramUniformHandleui64ARB(fp->getId(), l_tex, t->getHandle());


	param.albedo = glm::vec3(0.2, 0.7, 0.8);
	param.coeff = glm::vec4(0.2,0.8,1.0,100.0);
	param.specularColor = glm::vec3(1.0);


	// Drapeau sans vent
	physik.mass = 0.5f;
	physik.deltaTime = 0.0f;
	physik.kd_dampening = 12.0f;
	physik.ks_Stiffness = 6000.0f;
	physik.wind = 2.0f;
	physik.windFriction = 0.4f;

	// Drapeau avec vent (normal)
    //physik.mass = 0.3f;
    //physik.deltaTime = 0.0f;
    //physik.kd_dampening = 8.0f;
    //physik.ks_Stiffness = 12000.0f;
    //physik.wind = 10.0f;
    //physik.windFriction = 0.8f;

	// Drapeau avec vent (tempête)
    //physik.mass = 0.2f;
    //physik.deltaTime = 0.0f;
    //physik.kd_dampening = 6.0f;
    //physik.ks_Stiffness = 18000.0f;
    //physik.wind = 25.0f;
    //physik.windFriction = 1.2f;

	// Drapeau avec vent (doux)
    //physik.mass = 0.6f;
    //physik.deltaTime = 0.0f;
    //physik.kd_dampening = 15.0f;
    //physik.ks_Stiffness = 4000.0f;
    //physik.wind = 5.0f;
    //physik.windFriction = 0.5f;
	



	l_Time = glGetUniformLocation(vp->getId(), "Time");





	updatePhong();
}

MassSpringMaterial::~MassSpringMaterial()
{

}

void MassSpringMaterial::render(Node* o)
{


	m_ProgramPipeline->bind();

	o->drawGeometry(GL_TRIANGLES);
	m_ProgramPipeline->release();
}
void MassSpringMaterial::animate(Node* o, const float elapsedTime)
{

	glm::mat4 viewproj = Scene::getInstance()->camera()->getProjectionMatrix() * Scene::getInstance()->camera()->getViewMatrix();

	glProgramUniformMatrix4fv(vp->getId(), l_ViewProj, 1, GL_FALSE, glm::value_ptr(viewproj));
	glProgramUniformMatrix4fv(vp->getId(), l_Model, 1, GL_FALSE, glm::value_ptr(o->frame()->getModelMatrix()));

	glProgramUniform3fv(vp->getId(), l_PosLum, 1,  glm::value_ptr(Scene::getInstance()->getNode("Light")->frame()->convertPtTo(glm::vec3(0.0,0.0,0.0),o->frame())));

	glProgramUniform3fv(vp->getId(), l_PosCam, 1, glm::value_ptr(Scene::getInstance()->camera()->frame()->convertPtTo(glm::vec3(0.0, 0.0, 0.0), o->frame())));

	auto now_time = std::chrono::high_resolution_clock::now();
	auto timevar = now_time.time_since_epoch();
	float millis = 0.001f*std::chrono::duration_cast<std::chrono::milliseconds>(timevar).count();
	

	glProgramUniform1fv(vp->getId(), l_Time, 1,&millis);

	/*Direction du vecteur up dans le rep�re de l'objet. A utiliser pour d�finir la direction de la force de gravit�*/
	up_direction = Scene::getInstance()->getSceneNode()->frame()->convertDirTo(glm::vec3(0.0f, 1.0f, 0.0f), o->frame());

	/*Direction du vecteur Wind dans le rep�re de l'objet. A utiliser pour d�finir la direction de la force du vent*/
	wind_direction = Scene::getInstance()->getSceneNode()->frame()->convertDirTo(glm::vec3(0.0f, 0.0f, 1.0f), o->frame());
	
	/*Afin de d�coreller la simulation de l'afficahge , nous faisons ici 10 pas de simulation CPU avant d'afficher le r�sultat*/
	for (int i = 0;i < 10;i++)
		computeMassSpringAnimation((CustomModelGL*) o->getModel());

	/*Appels GPU pour mettre a jour le tableau des normales et des positions sur le GPU*/
	((CustomModelGL*)o->getModel())->recomputeNormals();
	((CustomModelGL*)o->getModel())->updatePositions();
}
void MassSpringMaterial::computeMassSpringAnimation(CustomModelGL* m)
{
    // Récupérer le modèle géométrique
    auto *model = m->getGeometricModel();

	// Créer les coefficients nécessaires pour les forces
    float damp = physik.kd_dampening; // Coefficient d'ammortissement 
    float wFr = physik.windFriction; // Friction du vent
    float mass = physik.mass;         // Masse de chaque particule
    float ks = physik.ks_Stiffness;  // Coefficient de rigidité des ressorts

    glm::vec3 gravity = -9.81f * up_direction; // Coefficient de gravité terrestre
     
    glm::vec3 W = physik.wind * wind_direction; // Vent

	// Reset des forces et ajout de la gravité
	for (int i = 0; i < m->V.size(); i++)
	{
        m->F[i] = glm::vec3(0.0f);

		// Ajout de la gravité
		m->F[i] += mass * gravity;

		// Ammortissement
        m->F[i] += -damp * m->V[i];
	}
	
	/*Ajouter les forces des ressorts*/
	computeAllSpringForces(m);

	// Friction de l'air
	for (int i = 0; i < model->nb_vertex; i++)
	{
        glm::vec3 N = model->listNormals[i];
        glm::vec3 Vrel = W - m->V[i];

		m->F[i] += -wFr * (glm::dot(N, Vrel)) * N;
	}
	
	// Contraintes
    for (int i = 0; i < m->m_nbElements; i++) {
        int idx = m->indice(i, 0);
        model->listVertex[idx] = m->restPositions[idx];
        m->F[idx] = glm::vec3(0.0f);
        m->V[idx] = glm::vec3(0.0f);
    };

	updateSimulation(m);
}


void MassSpringMaterial::computeAllSpringForces(CustomModelGL* m)
{

	for (Spring s : m->springs)
	{
		computeSpringForce(m,s);
	}

}

void MassSpringMaterial::updateSimulation(CustomModelGL* m)
{
    // Récupérer le modèle géométrique
    auto *model = m->getGeometricModel();

	// Intégration Euler semi-implicite (selon notes)
    float h = physik.deltaTime > 0 ? physik.deltaTime : 0.001f;  // Timestep
	for (int i = 0; i < m->V.size(); i++) // On boucle sur la taille des vitesses des sommets (vertex)
	{
        m->V[i] += h * (m->F[i] / physik.mass); // Mettre à jour la vitesse v_i(t+1) = v_i(t) + h*F_i
        model->listVertex[i] += h * m->V[i]; // Mettre à jour la position p_i(t+1) = p_i(t) + h*v_i(t+1)
	}
	
}

void MassSpringMaterial::computeSpringForce(CustomModelGL* m, Spring s)
{
    // Récupérer le modèle géométrique
    auto *model = m->getGeometricModel();

	// Force d'un ressort (slides 40 à 42 du chapitre 3)

	// Récupérer les index du ressort (id et id2) -> correspondront à respectivement i et j (sommets comme étant une masse, 
	// considéré comme particules)

	int i = s.id1;
    int j = s.id2;

	// On récupère la position des sommets selon leur index
    glm::vec3 pi = model->listVertex[i];
    glm::vec3 pj = model->listVertex[j];

	// On calcule la direction avec la position_i et position_j
    glm::vec3 d = pj - pi;
    float L = glm::length(d); // Changement d'un vecteur en scalaire par distance euclidienne

	if (L < 1e-6f) return; // Éviter une division par zéro lors de la normalisation de la direction avec une tolérance

	glm::vec3 dir = d / L; // Direction normalisé

	// On récupère la longueur au repos, le facteur de rigidité et d'ammortissement
    float L_0 = s.length;
    float k = physik.ks_Stiffness * s.KsFactor;
    float c = 0.1f;

	// Vélocité relative du ressort
    glm::vec3 vrel = m->V[j] - m->V[i];
    float vproj = glm::dot(vrel, dir);

	// On calcule la force
    glm::vec3 F = 
		k * (L - L_0) * dir // Force élastique
		- c * vproj * dir;  // Force d'ammortissement

	// On applique la force aux deux sommets selon la troisième loi de Newton
    m->F[i] += F; // Action
    m->F[j] -= F; // Réaction
}


void MassSpringMaterial::updatePhong()
{
	glProgramUniform4fv(fp->getId(), l_Phong, 1, glm::value_ptr(glm::vec4(param.coeff)));
	glProgramUniform3fv(fp->getId(), l_Albedo, 1, glm::value_ptr(param.albedo));
	glProgramUniform3fv(fp->getId(), l_specColor, 1, glm::value_ptr(param.specularColor));
}





void MassSpringMaterial::displayInterface()
{

	if (ImGui::TreeNode("MassSpring"))
	{
			ImGui::BeginGroup();
			ImGui::SliderFloat("Particule Mass", &physik.mass, 0.05f, 10.0f, "Mass = %.3f");
			ImGui::SliderFloat("DeltaTime", &physik.deltaTime, 0.0f, 0.001f, "DeltaTime = %.4f");
			ImGui::SliderFloat("Stiffness : ks", &physik.ks_Stiffness, 0.0f, 20000.0f, "Stiffness = %.2f");
			ImGui::SliderFloat("Dampening", &physik.kd_dampening, 0.0f, 20.0f, "Dampening = %.2f");
			ImGui::SliderFloat("Wind power", &physik.wind, 0.0f, 50.0f, "Wind = %.2f");
			ImGui::SliderFloat("Wind Friction", &physik.windFriction, 0.0f, 2.0f, "Wind Friction = %.2f");

			ImGui::EndGroup();
			ImGui::Separator();
			ImGui::Spacing();

			ImGui::TreePop();
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




