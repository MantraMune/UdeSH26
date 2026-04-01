import LinearAlgebra
export steepest

# Algorithme de descente du gradient classique.  Basez-vous sur ce gabarit pour implémenter BFGS.
function steepest(	nlp :: AbstractNLPModel;
					linesearch :: Function,
					kwargs...)

	# Déclaration des fonctions utiles à l'algorithme
	f(x) = obj(nlp,x)
	nabf(x) = grad(nlp,x)

	# Certaines initialisations
	xk=Array{Float64,1}
	nabf_k = Array{Float64,1}
	xk= copy(nlp.meta.x0)	
	nabf_k=nabf(xk)
	n=nlp.meta.nvar

	iter =0
	optimal=false
	stalled=false
	tired=false
	
	# Vérification si le point courant est déjà optimal
	if norm(nabf_k)<10^-5
		return xk, f(xk), nabf_k, iter, optimal, tired
	end

	# Déclaration de la structure du modèle linéaire
	h= LineModel(nlp, xk, -nabf_k)

	# Boucle principale : Votre code viens ici, la plupart des "0" sont des formules à coder. 
	while (!(stalled || optimal || tired ))
		# Certains calculs : valeur de fonction, direction et pente
		fxk= f(xk)		# Valeur de la fonction à ce point # Gradient au point courant
		d= -nabf_k		# Définition de votre direction
		slope = dot(nabf_k, d);	# Définition de la pente
		if slope >= 0.0
			stalled=true;
			status=:Stalled;
			println("Not a descent direction")
		else

			# Déclaration du modèle linéaire pour la recherche linéaire
			h= LineModel(nlp, xk, d)

			# Calcul du pas admissible 
			t= linesearch(h, fxk, slope)

			# Mise à jour incrémentation et mise à jour des différentes valeurs 
			xk = xk + t * d				# Incrémentation de xk
			nabf_k= nabf(xk);				# Mise à jour du gradient
			optimal=(norm(nabf_k)<10^-6)		# Critère d'arrêt d'optimalité
			tired=(iter>10000)			# Critère d'arrêt d'épuisement
			iter=iter+1;
		end

	end
	
	# Vérification si l'algorithme a trouvé une solution ou a dépassé la quantité maximale d'itérations permise
	if optimal
		status=:Optimal
		@show iter
	elseif tired
		status=:Tired
	end

	# Solution obtenue
	return xk, f(xk), nabf_k, iter, status

end
