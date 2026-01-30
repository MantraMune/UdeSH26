
using JuMP, NLPModels, NLPModelsJuMP, LinearAlgebra, ADNLPModels, JSOSolvers

# Relations de la fonction objectif sous forme || X*p-Y ||_1
	# X = zeros(3); 		# coefficients de p (vu qu'on rentre les poids dans les valeurs absolues, la variable ne sert à rien)
Y = [100.0, 22.0, 5.0]; # Formule en a)
x0 = [0.0,1.0,1.0,1.0]; # Valeurs initiales pour p (peut être quelconques)

# Transformation en problème différentiable
f(x) = x[2] + x[3] + x[4] # u1 + u2 + u3
c(x) = [            # Contraintes (a)
    100.0 - x[1] - x[2];
    x[1] - 100.0 - x[2];
    22.0 - x[1]/4 - x[3];
    x[1]/4 - 22.0 - x[3];
    5.0 - x[1]/25 - x[4];
    x[1]/25 - 5.0 - x[4]
];
lb = [-Inf, 0.0, 0.0, 0.0]; # Bornes inférieures des contraintes (a)
ub = [Inf, Inf, Inf, Inf]; # Bornes supérieures des contraintes (a)

# Élaboration du modèle
nlp = ADNLPModel(f, x0, c=c; lcon=lb, ucon=ub);

# Résolution du modèle par lbfgs
res = lbfgs(nlp);

# Affichage des résultats
println("Solution du modèle en utilisant lbfgs", "\n");
println("Valeurs de la solution : ", res.solution);

# Calcul de la valeur de la fonction objectif initiale
xopt = res.solution;
println("Valeur de la fonction objectif: ", f(xopt));

# Calcul de la dérivée au point optimal
gx = NLPModels.grad(nlp, xopt);
println("Dérivée au point optimal = ", gx);
