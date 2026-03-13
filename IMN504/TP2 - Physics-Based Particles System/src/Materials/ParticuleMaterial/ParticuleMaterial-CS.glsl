#version 460

layout(local_size_x = 256) in;

// -- Buffers (ping-pong) ---
// Source et cible pour les positions
layout(std430, binding = 0) buffer PosSrc { vec4 positionsSrc[]; };
layout(std430, binding = 1) buffer PosTrg { vec4 positionsTrg[]; };

// Source et cible pour les vitesses
layout(std430, binding = 2) buffer VelSrc { vec4 velocitiesSrc[]; };
layout(std430, binding = 3) buffer VelTrg { vec4 velocitiesTrg[]; };

// Table de hachage (collisions)
layout(std430, binding = 4) buffer HashTable { int hashTable[]; };

// --- Variables uniformes ---
uniform float DeltaTime;
uniform vec3 GravityDir;
uniform float Radius; // rayon d'une particule
uniform float BoxHalfSize; // moitié de la boîte de collision
uniform float CellSize; // Taille d'une cellule de la table
uniform ivec3 GridDim; // Nombre de cellules en x, y et z

// --- Fonctions ---
uint hashPosition(vec3 pos)
{
	ivec3 cell = ivec3(floor(pos / CellSize)) + ivec3(GridDim) / 2;
	return uint(clamp(cell.x, 0, GridDim.x - 1)
			  + clamp(cell.y, 0, GridDim.y - 1) * GridDim.x
			  + clamp(cell.z, 0, GridDim.z - 1) * GridDim.x * GridDim.y);
}

void main() {
	uint idx = gl_GlobalInvocationID.x;

	// Sécurité (pas dépasser le tableau)
	if (idx >= positionsSrc.length()) return;

	// Lire la position et la vitesse
	vec4 pos = positionsSrc[idx];
	vec4 vel = velocitiesSrc[idx];

	// Gravité simple sur Y
	vel.xyz += GravityDir * 9.81 * DeltaTime;

	// Intégration simple (Euler)
	pos.xyz += vel.xyz * DeltaTime;

	// Collisions avec les bords de la boîte
	for (int i = 0; i < 3; i++)
	{
		if (pos[i] - Radius < -BoxHalfSize)
		{
			pos[i] = -BoxHalfSize + Radius;
			vel[i] *= -1.0;
		}
		else if (pos[i] + Radius > BoxHalfSize)
		{
			pos[i] = BoxHalfSize - Radius;
			vel[i] *= -1.0;
		}
	}

	// Insertion dans la table de hachage
	vec3 offset = vec3(CellSize * 0.5);

	uint cellIndex1 = hashPosition(pos.xyz);
	uint cellIndex2 = hashPosition(pos.xyz + offset);

	atomicExchange(hashTable[cellIndex1], int(idx));
	atomicExchange(hashTable[cellIndex2], int(idx));

	// Collisions particules <-> particules
	ivec3 cells[2];
	cells[0] = ivec3(floor(pos.xyz / CellSize)) + ivec3(GridDim) / 2;
	cells[1] = ivec3(floor((pos.xyz + offset) / CellSize)) + ivec3(GridDim) / 2;
	for (int c = 0; c < 2; c++)
	{
		ivec3 cell = cells[c];

		for (int dx=-1; dx <= 1; dx++)
		for (int dy=-1; dy <= 1; dy++)
		for (int dz=-1; dz <= 1; dz++)
		{
			ivec3 neighborCell = cell + ivec3(dx,dy,dz);
			neighborCell = clamp(neighborCell, ivec3(0), GridDim-1);
			uint neighborIndex = uint(neighborCell.x + neighborCell.y*GridDim.x + neighborCell.z*GridDim.x*GridDim.y);
			int other = hashTable[neighborIndex];
			if(other >=0 && other != int(idx))
			{
				vec3 delta = pos.xyz - positionsSrc[other].xyz;
				float dist2 = dot(delta, delta);
				float r = 2.0 * Radius;
				if (dist2 < r * r)
				{
					float dist = sqrt(dist2) + 1e-6;
					vec3 dir = delta / dist;
					
					float vRel = dot(vel.xyz - velocitiesSrc[other].xyz, dir);
					vec3 vChange = 0.5 * vRel * dir;
					
					vel.xyz -= vChange;

					float penetration = r - dist;
					pos.xyz += dir * 0.5 * penetration;
				}
			}
		}
	}

	// Écrire dans le buffer cible
	positionsTrg[idx] = pos;
	velocitiesTrg[idx] = vel;
}