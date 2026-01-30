
import Pkg
Pkg.activate("C:\\Users\\ashry\\Downloads\\Packages")
using LinearAlgebra
using ADNLPModels
using JSOSolvers
using NLPModels

# Définir les points P, Q et le point de départ de l'algorithme
x0 = [0.0]
Ps = [               # P contient une liste de points 2D pour montrer les exemples utilisés dans le modèle
    [1.0; 2.0],
    [2.0; 4.0],
    [-1.0; 3.0]]
Qs = [               # Q contient une liste de points 2D pour montrer les exemples utilisés dans le modèle
    [2.0; 1.0],
    [3.0; 3.0],
    [0.0; 4.0]]

for i in axes(Ps, 1)
   P = Ps[i];   # Boucle lisant chaque paire de points (P, Q)
   Q = Qs[i]; 

   # Définition de la fonction à optimiser (minimiser)
   f(x) = begin
        X = [x[1]; 0.0]
        norm(P - X, 2) + norm(Q - X, 2)
   end
   
   # Élaboration du modèle
   nlp = ADNLPModel(f, x0)

   # Optimisation avec LBFGS
   stats = lbfgs(nlp)
   xopt = stats.solution

   println("Exemple ", i)
   println("Solution X avec LBFGS = ", xopt)

   # Affichage de la valeur de la fonction objectif au point optimal
   println("Valeur de la fonction objectif = ", stats.objective)

   # Dérivée au point optimal
   gx = NLPModels.grad(nlp, xopt)
   println("Dérivée au point optimal = ", gx)

   # Calcul des angles
   thetaP = atan(abs(P[2]) / abs(xopt[1] - P[1]))
   thetaQ = atan(abs(Q[2]) / abs(xopt[1] - Q[1]))

   # Affichage des angles
   println("Angle θP = ", thetaP * 180 / pi, " degrés")
   println("Angle θQ = ", thetaQ * 180 / pi, " degrés")
end

