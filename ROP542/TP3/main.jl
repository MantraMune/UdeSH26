# Document qui englobe les imports pour la résolution d'un problème d'optimisation général.  Les différents paramètres sont modifiés dans ce main.
# Ce document cherche à faire une implémentation intuitive de méthodes d'optimisation simples sur des problèmes connus.
# Certains packages sont utilisés, mais l'idée reste de donner une intuition rapide sur des implémentations simples.

# Importation des packages utilisés
using JuMP, NLPModels, LinearAlgebra, ADNLPModels

# Lecture du problème à minimiser
#name = :genrose_autodiff
#name = :arwhead
#name = :cragglvy
name = :fletcbv2
#name = :woods

# Import de la fonction à optimiser.
include("Fonctions_test/$(String(name)).jl")

# Quelques utilitaires
# include("IpoptOverhead.jl")
include("LineSearch.jl")

# Import des deux fonctions à comparer
include("steepest.jl")
include("BFGS.jl")

#Choix de la méthode à utiliser pour la recherche linéaire
#LineSearch=ArmijoLineSearch 	# Procédé d'Armijo classique
LineSearch=Newarmijo_wolfe     # Nouveau procédé de Armijo et Wolfe




# Différents paramètres à utiliser
n=500     	            # Quantité de variables du problème
nlp=eval(name)(n)       # Création du NLPModel avec la fonction lue dans name


#Affichage des choix effectués préalaablement
println(" Exemple avec le test ", name, " de dimension ", n, " avec la recherche linéaire suivante : $LineSearch")

# Affichage des résultats de la méthode du gradient
println("\n\n steepest\n")
@time xsteep, fsteep, n∇fsteep, itersteep, statussteep = steepest(nlp, linesearch=LineSearch)
# println(xsteep, fsteep, n∇fsteep,  statussteep)
if(n <= 5) println("Valeur optimale : ",xsteep, "Valeur de la fonction objectif : ",fsteep, "      Norme Gradient : ", norm(n∇fsteep), "       Status : ", statussteep)
else println("Valeur de la fonction objectif : ",fsteep, "      Norme Gradient : ", norm(n∇fsteep), "       Status : ", statussteep)
end

# Affichage des résultats de la méthode du BFGS
println("\n\n bfgs \n")
@time xBFGS, fBFGS, n∇fBFGS, iterBFGS, statusBFGS = BFGS(nlp, linesearch=LineSearch)
# println(xBFGS, fBFGS, n∇fBFGS,  statusBFGS)
if(n <= 5) println("Valeur optimale : ",xBFGS, "Valeur de la fonction objectif : ",fBFGS, " Norme Gradient : ", norm(n∇fBFGS), "       Status : ", statusBFGS)
else println("Valeur de la fonction objectif : ",fBFGS, " Norme Gradient : ", norm(n∇fBFGS), "       Status : ", statusBFGS)
end
