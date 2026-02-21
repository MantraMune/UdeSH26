#!/usr/bin/env python3
"""
Générateur de fichiers CSV pour les tests TP1
Système de Transport STS Sherbrooke
Données déterministes pour évaluation cohérente

Usage:
    python3 generer_csv_tests.py [repertoire_destination]
    
Exemple:
    python3 generer_csv_tests.py src/main/resources/
"""

import csv
import sys
from pathlib import Path

# Seed déterministe (pas utilisé actuellement mais disponible si besoin)
SEED = 630

# Données STS Sherbrooke
LIGNES_STS = {
    1: {"nom": "Ligne01", "arrets": ["Universite", "Cegep", "Centre-ville", "Gare"]},
    2: {"nom": "Ligne02", "arrets": ["Universite", "Mont-Bellevue", "Fleurimont", "Centre-ville"]},
    3: {"nom": "Ligne03", "arrets": ["Rock-Forest", "Centre-ville", "Jacques-Cartier"]},
    4: {"nom": "Ligne04", "arrets": ["Lennoxville", "Universite", "Centre-ville"]},
    5: {"nom": "Ligne05", "arrets": ["Universite", "Carrefour", "Centre-ville"]},
    6: {"nom": "Ligne06", "arrets": ["Universite", "Mont-Bellevue", "Centre-ville", "Rock-Forest"]},
}

BUS_CAPACITES = {
    "B011": 40, "B012": 40,
    "B021": 45, "B022": 45, "B023": 45, "B024": 45, "B025": 45,
    "B031": 50,
    "B041": 40, "B042": 40,
    "B051": 35, "B052": 35,
    "B061": 40, "B062": 40, "B063": 40, "B064": 40, "B065": 40,
    "B066": 40, "B067": 40, "B068": 40, "B069": 40, "B070": 40,
}

def ecrire_csv(nom_fichier, events, repertoire="."):
    """Écrit les événements dans un fichier CSV"""
    chemin = Path(repertoire) / nom_fichier
    chemin.parent.mkdir(parents=True, exist_ok=True)
    
    with open(chemin, 'w', newline='', encoding='utf-8') as f:
        writer = csv.writer(f)
        writer.writerow(['timestamp', 'type', 'passagerId', 'busId', 'ligne', 'arret', 'montant', 'methodePaiement'])
        for event in events:
            writer.writerow(event)

def generer_scenario_simple():
    """
    Test 1: Validation basique
    - 3 passagers, 2 bus
    - Voyages complets simples
    - Résultats attendus: 7.50$, 3 transportés
    """
    events = []
    ts = 1737532800000  # Base timestamp
    
    ligne = LIGNES_STS[1]
    arrets = ligne["arrets"]
    
    # Passager 1: Universite -> Centre-ville (B011)
    events.extend([
        (ts, "ENTREE_SYSTEME", "P0001", "", "", "", "", ""),
        (ts + 1000, "PAIEMENT", "P0001", "", "", "", "2.50", "CARTE"),
        (ts + 5000, "PASSAGER_MONTE", "P0001", "B011", ligne["nom"], arrets[0], "", ""),
        (ts + 10000, "BUS_DEPART", "", "B011", ligne["nom"], arrets[0], "", ""),
        (ts + 15000, "BUS_ARRIVEE", "", "B011", ligne["nom"], arrets[2], "", ""),
        (ts + 16000, "PASSAGER_DESCEND", "P0001", "B011", ligne["nom"], arrets[2], "", ""),
    ])
    
    # Passager 2: Universite -> Centre-ville (B011)
    events.extend([
        (ts + 100, "ENTREE_SYSTEME", "P0002", "", "", "", "", ""),
        (ts + 1100, "PAIEMENT", "P0002", "", "", "", "2.50", "ESPECES"),
        (ts + 5100, "PASSAGER_MONTE", "P0002", "B011", ligne["nom"], arrets[0], "", ""),
        (ts + 16100, "PASSAGER_DESCEND", "P0002", "B011", ligne["nom"], arrets[2], "", ""),
    ])
    
    # Passager 3: Cegep -> Gare (B012)
    events.extend([
        (ts + 200, "ENTREE_SYSTEME", "P0003", "", "", "", "", ""),
        (ts + 1200, "PAIEMENT", "P0003", "", "", "", "2.50", "CARTE"),
        (ts + 20000, "PASSAGER_MONTE", "P0003", "B012", ligne["nom"], arrets[1], "", ""),
        (ts + 22000, "BUS_DEPART", "", "B012", ligne["nom"], arrets[1], "", ""),
        (ts + 25000, "BUS_ARRIVEE", "", "B012", ligne["nom"], arrets[3], "", ""),
        (ts + 26000, "PASSAGER_DESCEND", "P0003", "B012", ligne["nom"], arrets[3], "", ""),
        (ts + 30000, "BUS_DEPART", "", "B012", ligne["nom"], arrets[3], "", ""),
    ])
    
    return sorted(events, key=lambda x: x[0])

def generer_scenario_compteurs():
    """
    Test 2: Race conditions sur compteurs
    - 50 passagers avec timestamps IDENTIQUES
    - Force la concurrence sur tous les compteurs
    - Résultats attendus: 125.00$, 50 transportés, tous bus vides
    """
    events = []
    ts = 1737532800000
    ligne = LIGNES_STS[3]
    arrets = ligne["arrets"]
    bus_ids = ["B021", "B022", "B023", "B024", "B025"]
    
    # Tous les événements au MÊME timestamp pour forcer race conditions
    ts_entree = ts
    ts_paiement = ts + 1000
    ts_montee = ts + 5000
    ts_descente = ts + 10000
    
    for i in range(1, 51):
        pid = f"P{i:04d}"
        bus_id = bus_ids[(i - 1) % 5]
        
        events.extend([
            (ts_entree, "ENTREE_SYSTEME", pid, "", "", "", "", ""),
            (ts_paiement, "PAIEMENT", pid, "", "", "", "2.50", "CARTE"),
            (ts_montee, "PASSAGER_MONTE", pid, bus_id, ligne["nom"], arrets[0], "", ""),
            (ts_descente, "PASSAGER_DESCEND", pid, bus_id, ligne["nom"], arrets[2], "", ""),
        ])
    
    return sorted(events, key=lambda x: x[0])

def generer_scenario_capacite():
    """
    Test 3: Gestion de capacité
    - Bus B031 capacité 50 places
    - 60 tentatives de montée -> 10 refus
    - 30 descentes -> occupation finale = 20
    - Alerte à 90% (45 passagers)
    - Résultats attendus: 150.00$, 50 transportés, 10 refus, alerte OUI
    """
    events = []
    ts = 1737532800000
    ligne = LIGNES_STS[3]
    arrets = ligne["arrets"]
    
    # 60 passagers tentent de monter dans B031
    for i in range(1, 61):
        pid = f"P{i:04d}"
        
        events.extend([
            (ts + i * 10, "ENTREE_SYSTEME", pid, "", "", "", "", ""),
            (ts + 1000 + i * 10, "PAIEMENT", pid, "", "", "", "2.50", "CARTE"),
            (ts + 5000 + i * 10, "PASSAGER_MONTE", pid, "B031", ligne["nom"], arrets[0], "", ""),
        ])
    
    # Seulement les 30 premiers descendent (occupation finale = 20)
    for i in range(1, 31):
        pid = f"P{i:04d}"
        events.append((ts + 10000 + i * 10, "PASSAGER_DESCEND", pid, "B031", ligne["nom"], arrets[2], "", ""))
    
    return sorted(events, key=lambda x: x[0])

def generer_scenario_deadlock():
    """
    Test 4: Absence de deadlock
    - 10 passagers font des transferts simultanés B041 -> B042
    - Restent dans B042 (pas de descente finale)
    - Résultats attendus: 25.00$, 10 transportés, B042=10
    """
    events = []
    ts = 1737532800000
    ligne = LIGNES_STS[4]
    arrets = ligne["arrets"]
    
    # Phase 1: Entrées et paiements
    for i in range(1, 11):
        pid = f"P{i:04d}"
        events.extend([
            (ts + i * 10, "ENTREE_SYSTEME", pid, "", "", "", "", ""),
            (ts + 1000 + i * 10, "PAIEMENT", pid, "", "", "", "2.50", "CARTE"),
        ])
    
    # Phase 2: Montées initiales dans B041
    for i in range(1, 11):
        pid = f"P{i:04d}"
        events.append((ts + 5000 + i * 10, "PASSAGER_MONTE", pid, "B041", ligne["nom"], arrets[0], "", ""))
    
    # Phase 3: Transferts SIMULTANÉS (potentiel deadlock)
    ts_transfert = ts + 10000
    for i in range(1, 11):
        pid = f"P{i:04d}"
        events.extend([
            (ts_transfert, "PASSAGER_DESCEND", pid, "B041", ligne["nom"], arrets[1], "", ""),
            (ts_transfert, "PASSAGER_MONTE", pid, "B042", ligne["nom"], arrets[1], "", ""),
        ])
    
    # Phase 4: PAS de descentes finales - les 10 restent dans B042
    
    return sorted(events, key=lambda x: x[0])

def generer_scenario_voyage():
    """
    Test 5: Logique métier - Patterns variés
    - Cas 1: Voyage complet (monte + descend)
    - Cas 2: Monte seulement
    - Cas 3: Descend sans monter
    - Cas 4: Monte et descend 2 fois
    - Résultats attendus: 3 transportés (seulement voyages complets)
    """
    events = []
    ts = 1737532800000
    ligne = LIGNES_STS[5]
    arrets = ligne["arrets"]
    
    # Cas 1: Voyage complet normal (P0001)
    events.extend([
        (ts, "ENTREE_SYSTEME", "P0001", "", "", "", "", ""),
        (ts + 1000, "PAIEMENT", "P0001", "", "", "", "2.50", "CARTE"),
        (ts + 5000, "PASSAGER_MONTE", "P0001", "B051", ligne["nom"], arrets[0], "", ""),
        (ts + 10000, "PASSAGER_DESCEND", "P0001", "B051", ligne["nom"], arrets[2], "", ""),
    ])
    
    # Cas 2: Monte mais ne descend jamais (P0002) - PAS transporté
    events.extend([
        (ts + 100, "ENTREE_SYSTEME", "P0002", "", "", "", "", ""),
        (ts + 1100, "PAIEMENT", "P0002", "", "", "", "2.50", "CARTE"),
        (ts + 5100, "PASSAGER_MONTE", "P0002", "B051", ligne["nom"], arrets[0], "", ""),
    ])
    
    # Cas 3: Descend sans être monté (P0003) - PAS transporté
    events.extend([
        (ts + 200, "ENTREE_SYSTEME", "P0003", "", "", "", "", ""),
        (ts + 1200, "PAIEMENT", "P0003", "", "", "", "2.50", "CARTE"),
        (ts + 10200, "PASSAGER_DESCEND", "P0003", "B051", ligne["nom"], arrets[1], "", ""),
    ])
    
    # Cas 4: Monte et descend 2 fois (P0004) - Compté 2 fois
    events.extend([
        (ts + 300, "ENTREE_SYSTEME", "P0004", "", "", "", "", ""),
        (ts + 1300, "PAIEMENT", "P0004", "", "", "", "2.50", "CARTE"),
        (ts + 5300, "PASSAGER_MONTE", "P0004", "B052", ligne["nom"], arrets[0], "", ""),
        (ts + 10300, "PASSAGER_DESCEND", "P0004", "B052", ligne["nom"], arrets[1], "", ""),
        (ts + 15300, "PASSAGER_MONTE", "P0004", "B052", ligne["nom"], arrets[1], "", ""),
        (ts + 20300, "PASSAGER_DESCEND", "P0004", "B052", ligne["nom"], arrets[2], "", ""),
    ])
    
    return sorted(events, key=lambda x: x[0])

def generer_scenario_stress():
    """
    Test 6: Robustesse
    - 200 passagers, 10 bus
    - Haute concurrence
    - 150 descentes -> 50 restent dans les bus
    - Résultats attendus: 500.00$, 150 transportés, occupation totale = 50
    """
    events = []
    ts = 1737532800000
    ligne = LIGNES_STS[6]
    arrets = ligne["arrets"]
    bus_ids = [f"B06{i}" for i in range(1, 11)]
    
    # 200 passagers
    for i in range(1, 201):
        pid = f"P{i:04d}"
        bus_id = bus_ids[(i - 1) % 10]
        
        events.extend([
            (ts + i, "ENTREE_SYSTEME", pid, "", "", "", "", ""),
            (ts + 1000 + i, "PAIEMENT", pid, "", "", "", "2.50", "CARTE"),
            (ts + 5000 + i, "PASSAGER_MONTE", pid, bus_id, ligne["nom"], arrets[0], "", ""),
        ])
    
    # Seulement 150 descendent (50 restent)
    for i in range(1, 151):
        pid = f"P{i:04d}"
        bus_id = bus_ids[(i - 1) % 10]
        events.append((ts + 10000 + i, "PASSAGER_DESCEND", pid, bus_id, ligne["nom"], arrets[2], "", ""))
    
    return sorted(events, key=lambda x: x[0])

def main():
    # Déterminer le répertoire de destination
    repertoire = sys.argv[1] if len(sys.argv) > 1 else "."
    
    print("=" * 70)
    print("Génération des fichiers CSV - Tests TP1")
    print("Système de Transport STS Sherbrooke")
    print("=" * 70)
    print()
    print(f"Répertoire de destination: {repertoire}")
    print()
    
    tests = [
        ("scenario_simple.csv", generer_scenario_simple, "Validation basique"),
        ("scenario_compteurs.csv", generer_scenario_compteurs, "Race conditions"),
        ("scenario_capacite.csv", generer_scenario_capacite, "Gestion capacité"),
        ("scenario_deadlock.csv", generer_scenario_deadlock, "Absence deadlock"),
        ("scenario_voyage.csv", generer_scenario_voyage, "Logique métier"),
        ("scenario_stress.csv", generer_scenario_stress, "Robustesse"),
    ]
    
    for nom_fichier, generateur, description in tests:
        events = generateur()
        ecrire_csv(nom_fichier, events, repertoire)
        print(f"✓ {nom_fichier:25s} {len(events):4d} événements - {description}")
    
    print()
    print("=" * 70)
    print("Tous les fichiers CSV générés avec succès!")
    print("=" * 70)
    print()
    print("Résultats attendus:")
    print("  scenario_simple     : 7.50$, 3 transportés")
    print("  scenario_compteurs  : 125.00$, 50 transportés (stable 10/10)")
    print("  scenario_capacite   : 150.00$, 50 transportés, 10 refus, alerte OUI")
    print("  scenario_deadlock   : 25.00$, 10 transportés (pas timeout)")
    print("  scenario_voyage     : 7.50$, 3 transportés")
    print("  scenario_stress     : 500.00$, 150 transportés (stable 45+/50)")

if __name__ == "__main__":
    main()
