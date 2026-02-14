

#ifdef USE_ASSIMP

#ifndef _ASSIMPLOADER_
#define _ASSIMPLOADER_

#include <assimp/Importer.hpp> // C++ importer interface
#include <assimp/scene.h> // Output data structure
#include <assimp/postprocess.h> // Post processing flags
#include "GeometricModelLoader/GeometricModelLoader.h"
using namespace std;
class GeometricModel;

class AssimpLoader : public GeometricModelLoader
{
	public:
		AssimpLoader();
		~AssimpLoader();
		virtual bool loadModel(string name,GeometricModel *model);
	protected:
		bool loadFromMesh(aiMesh* mesh, GeometricModel *model);
};



#endif

#endif
