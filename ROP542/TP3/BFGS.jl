import LinearAlgebra
export BFGS

function BFGS(	nlp :: AbstractNLPModel;
					linesearch :: Function,
					kwargs...)

	# Déclaration des fonctions utiles à l'algorithme
	f(x) = obj(nlp,x)
	nabf(x) = grad(nlp,x)

	# Certaines initialisations
	n=nlp.meta.nvar
	xt=Array{Float64,1}
	nabft = Array{Float64,1}
	n=nlp.meta.nvar
	Bk=	zeros(n,n)+I;
	xk= copy(nlp.meta.x0)	
	nabf_k=nabf(xk)
	nabf_kprec=nabf_k;
	
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

	# Boucle principale
	while (!(stalled || optimal || tired ))
		# Certains calculs : valeur de fonction, direction et pente
		fxk=f(xk);
		d= -Bk*nabf_k;
		slope = nabf_k'*d;
		if slope >0.0
			stalled=true;
			status=:Stalled;
			println("Not a descent direction")
		else

			# Déclaration du modèle linéaire pour la recherche linéaire
			h=redirect!(h,xk,d)

			# Calcul du pas admissible 
			t=linesearch(h, fxk, slope)

			# Mise à jour incrémentation et mise à jour des différentes valeurs 
			xkprec=xk;			
			xk= xk+t*d
			nabf_kprec=nabf_k;
			nabf_k=nabf(xk);
			
			yk=nabf_k-nabf_kprec;
			sk=xk-xkprec;
			#Hk=Hk+(yk*yk')/(yk'*sk)- (Hk*sk*sk'*Hk)/sk'*Hk*sk;
			if yk'*sk>10^-5*norm(nabf_k)*norm(sk)^2
				#BkFletcher=Bk+(I+(yk'*Bk*yk)/(sk'*yk))*(sk*sk')/(sk'*yk)-(sk*yk'*Bk+Bk*yk*sk')/(sk'*yk)
				Bk=(I-(sk*yk')/(yk'*sk))'*Bk*(I-(sk*yk')/(yk'*sk)) + (sk*sk')/(yk'*sk);
			end
			
			optimal=(norm(nabf_k)<10^-6)
			tired=(iter>1000000)
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
