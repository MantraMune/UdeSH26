
import Pkg
Pkg.activate("C:\\Users\\ashry\\Downloads\\Packages")
using LinearAlgebra
using ADNLPModels
using JSOSolvers
using NLPModels

# Définir les points P, Q et le point de départ de l'algorithme
x0 = [0.0]
P = [-1.0; 3.0]  # P contient un point 
Q = [2.0; -1.0]  # Q contient un point

# Définition de la fonction à optimiser (minimiser)
f(x) = begin
    X = [x[1]; 0.0]
    norm(P - X, 2)^2 + norm(Q - X, 2)^2
end
   
# Élaboration du modèle
nlp = ADNLPModel(f, x0)

# Optimisation avec LBFGS
stats = lbfgs(nlp)
xopt = stats.solution
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


