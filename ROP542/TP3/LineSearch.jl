include("line_model.jl")
export Newarmijo_wolfe
export ArmijoLineSearch
 
# Linesearch utilisant le procédé d'Armijo intuitif (tau1 est inutile).
function ArmijoLineSearch(nlp::LineModel,
						  f::Float64,
						  slope::Float64;
						  tau0::Float64 = 0.01,
						  tau1::Float64 = 0.95)
	theta=1.0;

	for i=1:100
		if ((obj(nlp,theta) -f - tau0*theta*slope ) <= 10^-5)
			return theta
		else
			theta=theta/2
		end
	end
	#error("LineSearch failed")
	
	return theta	
end

# LineSearch modifié qui ressemble au procédé d'Armijo, mais avec une plus grande chance d'avoir le critère de Wolfe satisfait.
# La procédure est surprennement efficace.
function Newarmijo_wolfe(h :: LineModel,
                         h₀ :: Float64,
                         slope :: Float64;
                         τ₀ :: Float64=1.0e-4,
                         τ₁ :: Float64=0.9999,
                         bk_max :: Int=50,
                         nbWM :: Int=50,
                         verboseLS :: Bool=false,
                         check_slope :: Bool = false,
                         kwargs...)

    if check_slope
      (abs(slope - grad(h, 0.0)) < 1e-4) || error("wrong slope")
      verboseLS && @show h₀ obj(h, 0.0) slope grad(h,0.0)
    end

    # Perform improved Armijo linesearch.
    nbk = 0
    nbW = 0
    t = 1.0

    # First try to increase t to satisfy loose Wolfe condition
    ht = obj(h, t)
    slope_t = grad(h, t)
    while (slope_t < τ₁*slope) && (ht <= h₀ + τ₀ * t * slope) && (nbW < nbWM)
        t *= 5.0
        ht = obj(h, t)
        slope_t = grad(h, t)

        nbW += 1
        verboseLS && println(" W  %4d  slope  %4d slopet %4d\n", nbW, slope, slope_t);
    end

    hgoal = h₀ + slope * t * τ₀;
    fact = -0.8
    ϵ = 1e-10

    # Enrich Armijo's condition with Hager & Zhang numerical trick
    Armijo = (ht <= hgoal) || ((ht <= h₀ + ϵ * abs(h₀)) && (slope_t <= fact * slope))
    while !Armijo && (nbk < bk_max)
        t *= 0.4
        ht = obj(h, t)
        hgoal = h₀ + slope * t * τ₀;

        # avoids unused grad! calls
        Armijo = false
        good_grad = false
        if ht <= hgoal
            Armijo = true
        elseif ht <= h₀ + ϵ * abs(h₀)
            slope_t = grad(h, t)
            if slope_t <= fact * slope
                Armijo = true
            end
        end

        nbk += 1
        verboseLS && println(" A  %4d  h0  %4e ht %4e\n", nbk, h₀, ht);
    end

    verboseLS && println("  %4d %4d %8e\n", nbk, nbW, t);
    stalled = (nbk == bk_max)
	if (t < 0.0) || (isnan(t))
		error("invalid step")
	end
    return t
end


