
using JuMP, NLPModels, NLPModelsJuMP, LinearAlgebra, ADNLPModels, JSOSolvers

# Relations de la fonction objectif sous forme || X*p-Y ||_1
	# X = zeros(3); 		# coefficients de p (vu qu'on rentre les poids dans les valeurs absolues, la variable ne sert à rien)
Y = [100.0, 88.0, 125.0]; # Formule en b)
x0 = [0.0,0.0,0.0,0.0]; # Valeurs initiales pour p (peut être quelconques)

# Transformation en problème différentiable
f(x) = x[2] + x[3] + x[4] # u1 + u2 + u3
c(x) = [            # Contraintes (b)
    100.0 - x[1] - x[2];
    x[1] - 100.01 - x[2];
    88.0 - x[1] - x[3];
    x[1] - 88.0 - x[3];
    125.0 -x[1] - x[4];
    x[1] - 125.0 - x[4]
];
lb = [-Inf, 0.0, 0.0, 0.0]; # Bornes inférieures des contraintes (b)
ub = [Inf, Inf, Inf, Inf]; # Bornes supérieures des contraintes (b)

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

