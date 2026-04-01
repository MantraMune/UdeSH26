# Rapport de performance entre la méthode de la pente la plus forte et BFGS (question #1 du TP #3)

par Luc Nathan Ramasamy et Emile Dagenais

---

Fonctions tests choisies : `genrose_autodiff` et `fletcbv2`

## Recherche linéaire d'Armijo

### Fonction : `genrose_autodiff`

#### N = 5

Pente la plus forte (steepest) : Temps d'exécution - 0.181330 s

Valeur optimale : [0.9999864992333222, 0.9998939559716593, 0.9999152740762283, 0.999702342110137, 0.9994822539047573]

Valeur de la fonction objectif : 1.0000045980004884      

Norme Gradient : 0.12190201643631093       

Status : Tired

BFGS : Temps d'exécution - 3.913716 s

Valeur optimale : [1.001635905344864, 1.0038116767536527,1.0072623967539363, 1.0144156439346173, 1.029491193232286]

Valeur de la fonction objectif : 1.0003437743567658 

Norme Gradient : 0.39843814737052136       

Status : Tired

On voit que les deux algorithmes ont abandonné (`Tired`) même après une taille de 5, car cela peut être dû à la recherche linéaire d'Armijo. En tout cas, la méthode de la pente la plus forte est celle qui est la plus rapide mais il n'a pas vraiment eu de convergence pour le moment.

#### N = 50

Pente la plus forte (steepest) : Temps d'exécution - 0.236560 s

Valeur de la fonction objectif : 1.0000028839727229      

Norme Gradient : 0.10155404989703905       

Status : Tired

BFGS : Temps d'exécution - 65.495884 s

Valeur de la fonction objectif : 1.0005801301747774 

Norme Gradient : 0.4893797115146146       

Status : Tired

Même constat qu'avec N = 5. La méthode de la pente la plus forte est la plus rapide mais aucune convergence, car le résultat obtenu n'est pas optimal, ce qui peut être dû à la méthode de recherche linéaire.

#### N = 500

Pente la plus forte (steepest) : Temps d'exécution - 2.381453 s

Valeur de la fonction objectif : 197.89718352888644      

Norme Gradient : 4.90043981744134       

Status : Tired

BFGS : Temps d'exécution - >65.495884 s (prend plus de temps)

Même sans les données de BFGS, on voit toujours le même constat. La méthode de la pente la plus forte est plus rapide que BFGS de loin mais toujours aucune convergence.

### Fonction : `fletcbv2`

#### N = 5

Pente la plus forte (steepest) : Temps d'exécution - 0.210751 s

Valeur optimale : [0.27104546588730577, 0.49534757455447115, 0.6749232995189476, 0.8190315717596927, 0.9255189521218823]

Valeur de la fonction objectif : -0.6887198889562385

Norme Gradient : 0.004731244274929842       

Status : Tired

BFGS : Temps d'exécution - 0.371497 s

Converge après 27932 itérations.

Valeur optimale : [0.271411965787556, 0.49471539950434645, 0.6756515020374176, 0.8184042037700607, 0.9258805903379056]

Valeur de la fonction objectif : -0.6887228705849758 

Norme Gradient : 2.9145824723983895e-7       

Status : Optimal

On voit ici qu'on a un effet différent avec une fonction de test différent. Ici, certes la méthode de la pente la plus forte reste la plus rapide mais elle ne converge pas comparé à BFGS qui elle converge (`Optimal`) malgré un temps un peu plus long.

#### N = 50

Pente la plus forte (steepest) : Temps d'exécution - 0.294091 s

Valeur de la fonction objectif : -0.5273940981615505      

Norme Gradient : 0.007443365145418903       

Status : Tired

BFGS : Temps d'exécution - 59.469338 s

Valeur de la fonction objectif : -0.52735929986458 

Norme Gradient : 0.009833434389052414       

Status : Tired

Ici, on voit un constat qui se répète avec la fonction de test précédente. Les deux algorithmes ne convergent pas à N = 50 et la pente la plus forte est la plus rapide.

#### N = 500

Pente la plus forte (steepest) : Temps d'exécution - 7.038203 s

Valeur de la fonction objectif : -0.5027315813126094      

Norme Gradient : 0.0058428830951580924       

Status : Tired

BFGS : Temps d'exécution -  > 59.469338 s (prend plus de temps)

Même constat qu'avec la fonction de test précédente sur la même taille, steepest est plus rapide que BFGS et aucun des deux ne convergent.

## Recherche linéaire d'Armijo + Wolfe (optimisation)

### Fonction : `genrose_autodiff`

#### N = 5

Pente la plus forte (steepest) : Temps d'exécution - 0.212123 s

Converge après 8597 itérations.

Valeur optimale : [0.9999998966992082, 0.9999997929265809, 0.9999995848288772, 0.9999991678968805, 0.9999983313537625]

Valeur de la fonction objectif : 1.0000000000009208      

Norme Gradient : 9.828307325990463e-7       

Status : Optimal

BFGS : Temps d'exécution - 0.142957 s

Converge après 14149 itérations

Valeur optimale : [0.9999999960698143, 0.9999999921202932, 0.9999999845242468, 0.9999999704607133, 0.9999999400510452]

Valeur de la fonction objectif : 1.0000000000000013 

Norme Gradient : 8.156503590118823e-7       

Status : Optimal

Comme on l'observe, les deux algorithmes convergent mais BFGS prend plus d'itérations avant de converger. En termes d'exécution, c'est BFGS qui est plus rapide que steepest. C'est intéressant de voir que les deux algorithmes convergent enfin avec la première fonction de test en ayant changé de recherche linéaire.

#### N = 50

Pente la plus forte (steepest) : Temps d'exécution - 0.291835 s

Valeur de la fonction objectif : 1.0000000000041622      

Norme Gradient : 6.613070320672205e-6       

Status : Tired

BFGS : Temps d'exécution - 0.769972 s

Converge après 14702 itérations

Valeur de la fonction objectif : 1.0000000000000002 

Norme Gradient : 8.791563403418941e-7       

Status : Optimal

Ici, on voit que BFGS a convergé mais pas steepest, malgré que la méthode de la pente la plus forte était la plus rapide. Donc, pour cette fonction de test avec N = 50, il est préférable de prendre BFGS pour avoir une réponse qui converge.

#### N = 500

Pente la plus forte (steepest) : Tempŝ d'exécution - 4.291894 s

Valeur de la fonction objectif : 126.61197998754605      

Norme Gradient : 8.38249392226876       

Status : Tired

BFGS : Temps d'exécution - >0.769972 s (prend plus de temps)

Même constat qu'avec la recherche linéaire d'Armijo, mais cela est normal que BFGS prend plus de temps à s'exécuter sur N = 500 à cause du fait que la méthode calcule une matrice hessienne, ce que la pente la plus forte ne fait pas (calcule des gradients). Les deux ne convergent donc pas et la pente la plus forte serait la plus rapide.

### Fonction : `fletcbv2`

#### N = 5

Pente la plus forte (steepest) : Temps d'exécution - 0.206005 s

Converge après 63 itérations

Valeur optimale : [0.2714113184530961, 0.4947140419175834, 0.6756500641117669, 0.8184028442182489, 0.9258799029735091]

Valeur de la fonction objectif : -0.6887228705846451      

Norme Gradient : 7.382098067175842e-7       

Status : Optimal

BFGS : Temps d'exécution - 0.060879 s

Converge après 39 itérations

Valeur optimale : [0.27141240989304033, 0.4947159382942232, 0.6756520884268338, 0.8184047645397344, 0.925880905180529]

Valeur de la fonction objectif : -0.6887228705845081 

Norme Gradient : 6.137758109064865e-7       

Status : Optimal

On voit que les deux algorithmes convergent, mais BFGS converge beaucoup plus vite que steepest. Aussi, BFGS est plus vite à exécuter avec cette fonction de test que steepest. Tout cela est plus cohérent quand on connaît les deux algorithmes, ce qui dans le cas actuel (N = 5), les résultats sont cohérents.

#### N = 50

Pente la plus forte (steepest) : Temps d'exécution - 0.275661 s

Converge après 3479 itérations

Valeur de la fonction objectif : -0.5274010295930354      

Norme Gradient : 9.558659328176177e-7       

Status : Optimal

BFGS : Temps d'exécution - 0.230282 s

Converge après 2338 itérations

Valeur de la fonction objectif : -0.5274010296726284 

Norme Gradient : 9.493789177326757e-7       

Status : Optimal

Ici, on voit le même cas qu'avec N = 5, soit que les deux algorithmes convergent, BFGS converge plus vite et est plus rapide que steepest.

#### N = 500

Pente la plus forte (steepest) : Temps d'exécution - 11.461417 s

Valeur de la fonction objectif : -0.5027444785207369      

Norme Gradient : 0.00019685643536295773       

Status : Tired

BFGS : Temps d'exécution - >0.230282 s (rien après 5 minutes)

On peut voir que BFGS gère très mal les problèmes à grande taille comme N = 500 tandis que steepest abandonne après un certain moment mais retourne un temps d'exécution (le résultat ne sera juste pas optimal ni convergent). Donc, les deux algorithmes ne convergent pas à N = 500 et steepest serait techniquement le plus rapide même si la résultat ne converge pas vers une réponse optimal.

En conclusion, ce qu'on peut voir selon les résultats, c'est que la méthode de la pente la plus forte (mérhode de la descente du gradient) est la plus rapide dans les problèmes de grande taille (malgré qu'il n'y a aucune convergence), ce qui est normal, car elle consomme moins d'espace mémoire à exécuter (utilise le principe du gradient -> moins cher). Pour le cas de la BFGS, elle est la plus rapide dans les problèmes de petite et de moyenne taille (montré par une plus grande rapiditié d'exécution et une convergence rapide), car elle consomme plus d'espace mémoire à exécuter (utilise le principe de la matrice hessienne -> plus cher).
