#!/usr/bin/env python3
"""
Script de correction automatique pour TP1 - Système de Transport Concurrent
IFT630 - Processus Concurrents et Parallélisme
Université de Sherbrooke

Version 2.1 - Fixes regex virgules
"""

import sys
import json
import subprocess
import re
import os
import time
from pathlib import Path
from typing import Dict, Any, List, Tuple

class CorrecteurTP1:
    def __init__(self, repertoire_etudiant: str):
        self.repertoire = Path(repertoire_etudiant)
        self.resultats_attendus = self.charger_resultats_attendus()
        self.note_totale = 0
        self.note_max = 100
        self.compilation_points = 10
        self.rapport = []
        self.details_tests = []
        self.tous_les_resultats = {}
        
    def charger_resultats_attendus(self) -> Dict[str, Any]:
        """Charge les résultats attendus depuis expected_results.json"""
        fichier_attendus = Path(__file__).parent / "expected_results.json"
        with open(fichier_attendus, 'r') as f:
            return json.load(f)
    
    def compiler_projet(self) -> bool:
        """Compile le projet Maven"""
        print("\n" + "="*70)
        print("ETAPE 1: COMPILATION")
        print("="*70)
        
        try:
            result = subprocess.run(
                ["mvn", "clean", "compile"],
                cwd=self.repertoire,
                capture_output=True,
                text=True,
                timeout=120
            )
            
            if result.returncode == 0:
                print("RESULTAT: COMPILATION REUSSIE")
                self.rapport.append(("Compilation", "REUSSIE", self.compilation_points))
                self.note_totale += self.compilation_points
                return True
            else:
                print("RESULTAT: ECHEC DE COMPILATION")
                print("\nErreurs:")
                print(result.stderr)
                self.rapport.append(("Compilation", "ECHEC", 0))
                self.note_totale = 0
                return False
                
        except subprocess.TimeoutExpired:
            print("RESULTAT: TIMEOUT (>120s)")
            self.rapport.append(("Compilation", "TIMEOUT", 0))
            return False
        except Exception as e:
            print(f"RESULTAT: ERREUR - {e}")
            self.rapport.append(("Compilation", f"ERREUR: {e}", 0))
            return False
    
    def executer_test(self, nom_test: str, fichier_csv: str) -> Dict[str, Any]:
        """Exécute un test et lit les résultats depuis le fichier JSON généré"""
        print(f"\n" + "-"*70)
        print(f"Test: {nom_test}")
        print("-"*70)
        
        try:
            # Exécuter le programme
            result = subprocess.run(
                ["mvn", "exec:java", 
                 "-Dexec.mainClass=udes.fsci.info.ift630.transport.Main",
                 f"-Dexec.args={fichier_csv}"],
                cwd=self.repertoire,
                stdout=subprocess.DEVNULL,
                stderr=subprocess.PIPE,
                text=True,
                timeout=100
            )
            
            # Lire les résultats depuis le fichier JSON généré
            fichier_json = self.repertoire / "logs" / f"resultats_{nom_test}.json"
            if fichier_json.exists():
                with open(fichier_json, 'r', encoding='utf-8') as f:
                    resultats = json.load(f)
                return resultats
            else:
                return {"erreur": "fichier JSON non trouvé"}
            
        except subprocess.TimeoutExpired:
            print(f"TIMEOUT lors de l'execution de {nom_test}")
            return {"erreur": "timeout"}
        except Exception as e:
            print(f"ERREUR lors de l'execution de {nom_test}: {e}")
            return {"erreur": str(e)}
    
    def parser_sortie(self, sortie: str) -> Dict[str, Any]:
        """Parse la sortie du programme pour extraire les résultats"""
        resultats = {}
        
        # Question 1: Revenus total
        match = re.search(r'Question 1.*?Reponse:\s*([\d,]+)', sortie, re.DOTALL)
        if match:
            resultats['revenus_total'] = float(match.group(1).replace(',', '.'))
        
        # Question 2: Passagers transportés
        match = re.search(r'Question 2.*?Reponse:\s*(\d+)', sortie, re.DOTALL)
        if match:
            resultats['passagers_transportes'] = int(match.group(1))
        
        # Question 3: Total montées
        match = re.search(r'Question 3.*?Reponse:\s*(\d+)', sortie, re.DOTALL)
        if match:
            resultats['total_montees'] = int(match.group(1))
        
        # Question 4: Total descentes
        match = re.search(r'Question 4.*?Reponse:\s*(\d+)', sortie, re.DOTALL)
        if match:
            resultats['total_descentes'] = int(match.group(1))
        
        # Question 5: Occupation des bus
        occupation = {}
        for match in re.finditer(r'^\s*(B\d+):\s*(\d+)\s*passagers', sortie, re.MULTILINE):
            bus_id = match.group(1)
            nb_passagers = int(match.group(2))
            occupation[bus_id] = nb_passagers
        if occupation:
            resultats['occupation_bus'] = occupation
        
        # Question 6: Refus
        refus = {}
        for match in re.finditer(r'(B\d+):\s*(\d+)\s*refus', sortie):
            bus_id = match.group(1)
            nb_refus = int(match.group(2))
            refus[bus_id] = nb_refus
        if refus:
            resultats['refus'] = refus
        
        # Question 7: Alertes
        alertes = {}
        for match in re.finditer(r'(B\d+):\s*(OUI|NON)', sortie):
            bus_id = match.group(1)
            a_alerte = match.group(2) == "OUI"
            alertes[bus_id] = a_alerte
        if alertes:
            resultats['alertes_90'] = alertes
        
        # Question 8: Invariants
        match = re.search(r'Conservation.*?:\s*(RESPECTE|VIOLE)', sortie)
        if match:
            resultats['invariant_conservation'] = match.group(1) == "RESPECTE"
        
        # Question 9: Revenus par méthode (ACCEPTER VIRGULES ET POINTS)
        revenus_methode = {}
        section = re.search(r'Question 9:.*?(?=Question 10:|$)', sortie, re.DOTALL)
        if section:
            for match in re.finditer(r'(CARTE|ESPECES|MOBILE):\s*([\d.,]+)', section.group(0)):
                methode = match.group(1)
                montant = float(match.group(2).replace(',', '.'))
                revenus_methode[methode] = montant
        if revenus_methode:
            resultats['revenus_methode'] = revenus_methode
        
        # Question 10: Occupation max atteinte
        occupation_max = {}
        section = re.search(r'Question 10:.*', sortie, re.DOTALL)
        if section:
            for match in re.finditer(r'^\s*Max\s+(B\d+):\s*(\d+)\s*passagers', section.group(0), re.MULTILINE):
                bus_id = match.group(1)
                max_occup = int(match.group(2))
                occupation_max[bus_id] = max_occup
        if occupation_max:
            resultats['occupation_max'] = occupation_max
        
        # Question 11: Embarquements par arrêt
        embarq_arret = {}
        section = re.search(r'Question 11:.*?(?=Question 12:|$)', sortie, re.DOTALL)
        if section:
            for match in re.finditer(r'^\s*([^:]+?):\s*(\d+)\s*embarquements', section.group(0), re.MULTILINE):
                arret = match.group(1).strip()
                count = int(match.group(2))
                embarq_arret[arret] = count
        if embarq_arret:
            resultats['embarq_arret'] = embarq_arret
        
        # Question 12: Débarquements par arrêt
        debarq_arret = {}
        section = re.search(r'Question 12:.*?(?=$)', sortie, re.DOTALL)
        if section:
            for match in re.finditer(r'^\s*([^:]+?):\s*(\d+)\s*debarquements', section.group(0), re.MULTILINE):
                arret = match.group(1).strip()
                count = int(match.group(2))
                debarq_arret[arret] = count
        if debarq_arret:
            resultats['debarq_arret'] = debarq_arret
        
        return resultats
    
    def comparer_resultats(self, nom_test: str, obtenus: Dict[str, Any], 
                           attendus: Dict[str, Any]) -> Tuple[int, int]:
        """Compare les résultats obtenus avec les attendus"""
        points = 0
        points_max = attendus.get('points_max', 15)
        
        # Distribution des points par critère
        points_par_critere = {
            'scenario_simple': {
                'revenus': 1, 'transportes': 2, 'montees': 1, 
                'descentes': 1, 'occupation_bus': 1, 'invariant': 1,
                'revenus_methode': 2, 'occupation_max': 1, 'embarq': 2, 'debarq': 3
            },
            'scenario_compteurs': {
                'revenus': 1, 'transportes': 2, 'montees': 2, 
                'descentes': 2, 'occupation_bus': 2, 'invariant': 2,
                'revenus_methode': 1, 'occupation_max': 1, 'embarq': 1, 'debarq': 1
            },
            'scenario_capacite': {
                'revenus': 1, 'transportes': 2, 'montees': 2, 
                'descentes': 2, 'occupation_bus': 2, 'invariant': 2,
                'revenus_methode': 1, 'occupation_max': 1, 'embarq': 1, 'debarq': 1
            },
            'scenario_stress': {
                'revenus': 1, 'transportes': 2, 'montees': 2, 
                'descentes': 2, 'occupation_bus': 1, 'invariant': 1,
                'revenus_methode': 1, 'occupation_max': 1, 'embarq': 1, 'debarq': 1
            },
            'scenario_voyage': {
                'revenus': 2, 'transportes': 4, 'montees': 1, 
                'descentes': 1, 'occupation_bus': 1, 'invariant': 1,
                'revenus_methode': 2, 'occupation_max': 1, 'embarq': 1, 'debarq': 1

            },
        }
        
        pts = points_par_critere.get(nom_test, {
            'revenus': 2, 'transportes': 2, 'montees': 1, 
            'descentes': 1, 'occupation_bus': 1, 'invariant': 1,
            'revenus_methode': 1, 'occupation_max': 1, 'embarq': 1, 'debarq': 1
        })
        
        print(f"\nComparaison des resultats:")
        
        # Revenus total
        if 'revenus_total' in obtenus and 'revenus_total' in attendus:
            if abs(obtenus['revenus_total'] - attendus['revenus_total']) < 0.01:
                print(f"  Revenus total: OK ({obtenus['revenus_total']:.2f}$) [{pts['revenus']} pts]")
                points += pts['revenus']
            else:
                print(f"  Revenus total: ERREUR [0/{pts['revenus']} pts]")
                print(f"    Attendu: {attendus['revenus_total']:.2f}$")
                print(f"    Obtenu:  {obtenus['revenus_total']:.2f}$")
        
        # Passagers transportés
        if 'passagers_transportes' in obtenus and 'passagers_transportes' in attendus:
            if obtenus['passagers_transportes'] == attendus['passagers_transportes']:
                print(f"  Passagers transportes: OK ({obtenus['passagers_transportes']}) [{pts['transportes']} pts]")
                points += pts['transportes']
            else:
                print(f"  Passagers transportes: ERREUR [0/{pts['transportes']} pts]")
                print(f"    Attendu: {attendus['passagers_transportes']}")
                print(f"    Obtenu:  {obtenus['passagers_transportes']}")
        
        # Montées et descentes
        if 'total_montees' in obtenus and 'total_montees' in attendus:
            if obtenus['total_montees'] == attendus['total_montees']:
                print(f"  Total montees: OK ({obtenus['total_montees']}) [{pts['montees']} pts]")
                points += pts['montees']
            else:
                print(f"  Total montees: ERREUR [0/{pts['montees']} pts]")
        
        if 'total_descentes' in obtenus and 'total_descentes' in attendus:
            if obtenus['total_descentes'] == attendus['total_descentes']:
                print(f"  Total descentes: OK ({obtenus['total_descentes']}) [{pts['descentes']} pts]")
                points += pts['descentes']
            else:
                print(f"  Total descentes: ERREUR [0/{pts['descentes']} pts]")
        
        # Occupation des bus
        if 'occupation_bus' in obtenus and 'occupation_bus' in attendus:
            # print(f"  DEBUG occupation: obtenus={obtenus.get('occupation_bus')}")
            # print(f"  DEBUG occupation: attendus={attendus.get('occupation_bus')}")
            occupation_ok = True
            for bus_id, attendu in attendus['occupation_bus'].items():
                if bus_id in obtenus['occupation_bus']:
                    if obtenus['occupation_bus'][bus_id] == attendu:
                        print(f"  Occupation {bus_id}: OK ({attendu} passagers)")
                    else:
                        print(f"  Occupation {bus_id}: ERREUR")
                        occupation_ok = False
                else:
                    print(f"  Occupation {bus_id}: MANQUANT")
                    occupation_ok = False
            if occupation_ok:
                print(f"    [{pts['occupation_bus']} pts]")
                points += pts['occupation_bus']
        
        # Invariant
        if 'invariant_conservation' in obtenus:
            if obtenus['invariant_conservation']:
                print(f"  Invariant conservation: RESPECTE [{pts['invariant']} pts]")
                points += pts['invariant']
            else:
                print(f"  Invariant conservation: VIOLE [0/{pts['invariant']} pts]")
        
        # Revenus par méthode
        if 'revenus_methode' in obtenus and 'revenus_methode' in attendus:
            methode_ok = True
            for methode, attendu in attendus['revenus_methode'].items():
                if methode in obtenus['revenus_methode']:
                    if abs(obtenus['revenus_methode'][methode] - attendu) < 0.01:
                        print(f"  Revenus {methode}: OK ({attendu:.2f}$)")
                    else:
                        print(f"  Revenus {methode}: ERREUR")
                        methode_ok = False
                else:
                    methode_ok = False
            if methode_ok:
                print(f"    [{pts['revenus_methode']} pts]")
                points += pts['revenus_methode']
        
        # Occupation max
        if 'occupation_max' in obtenus and 'occupation_max' in attendus:
            max_ok = all(
                obtenus['occupation_max'].get(bus) == val
                for bus, val in attendus['occupation_max'].items()
            )
            if max_ok:
                print(f"  Occupation max: OK [{pts['occupation_max']} pts]")
                points += pts['occupation_max']
            else:
                print(f"  Occupation max: ERREUR [0/{pts['occupation_max']} pts]")
        
        # Embarquements par arrêt
        if 'embarq_arret' in obtenus and 'embarq_arret' in attendus and pts['embarq'] > 0:
            embarq_ok = all(
                obtenus['embarq_arret'].get(arret) == val
                for arret, val in attendus['embarq_arret'].items()
            )
            # print(f"  DEBUG: obtenus={obtenus.get('embarq_arret')}")
            # print(f"  DEBUG: attendus={attendus.get('embarq_arret')}")
            # print(f"  DEBUG: embarq_ok={embarq_ok}")
            if embarq_ok:
                print(f"  Embarquements arret: OK [{pts['embarq']} pts]")
                points += pts['embarq']
            else:
                print(f"  Embarquements arret: ERREUR [0/{pts['embarq']} pts]")
        
        # Débarquements par arrêt
        if 'debarq_arret' in obtenus and 'debarq_arret' in attendus and pts['debarq'] > 0:
            # print(f"  DEBUG debarq: obtenus={obtenus.get('debarq_arret')}")
            # print(f"  DEBUG debarq: attendus={attendus.get('debarq_arret')}")
            debarq_ok = all(
                obtenus['debarq_arret'].get(arret) == val
                for arret, val in attendus['debarq_arret'].items()
            )
            if debarq_ok:
                print(f"  Debarquements arret: OK [{pts['debarq']} pts]")
                points += pts['debarq']
            else:
                print(f"  Debarquements arret: ERREUR [0/{pts['debarq']} pts]")
        
        print(f"\nPoints obtenus: {points}/{points_max}")
        return points, points_max
    
    def tester_repetitions(self, nom_test: str, fichier_csv: str, 
                           nb_repetitions: int = 10) -> bool:
        """Teste la stabilité des résultats"""
        print(f"\nTest de stabilite ({nb_repetitions} repetitions):")
        
        resultats_liste = []
        for i in range(nb_repetitions):
            resultats = self.executer_test(nom_test, fichier_csv)  # Utiliser le même nom de test
            if 'erreur' not in resultats:
                resultats_liste.append(resultats)
            time.sleep(0.1)  # Petit délai pour éviter les conflits de fichiers
        
        if len(resultats_liste) < nb_repetitions:
            print(f"  INSTABLE: Seulement {len(resultats_liste)}/{nb_repetitions} executions reussies")
            return False
        
        premier = resultats_liste[0]
        for i, res in enumerate(resultats_liste[1:], 2):
            if res != premier:
                print(f"  INSTABLE: Resultats differents entre execution 1 et {i}")
                return False
        
        print(f"  STABLE: {nb_repetitions}/{nb_repetitions} resultats identiques")
        return True
    
    def executer_tous_les_tests(self):
        """Exécute tous les tests ou charge depuis le JSON si disponible"""
        print("\n" + "="*70)
        print("ETAPE 2: TESTS FONCTIONNELS")
        print("="*70)
        
        # Vérifier si les fichiers JSON séparés existent
        repertoire_logs = self.repertoire / "logs"
        fichiers_json = list(repertoire_logs.glob("resultats_*.json"))
        if fichiers_json:
            print("Fichiers de résultats trouvés, chargement rapide...")
            self.charger_resultats_depuis_json(repertoire_logs)
        else:
            print("Aucun fichier de résultats trouvé, exécution des tests...")
            self.executer_tests_complets()
        
        # Écrire les résultats dans le JSON global (au cas où ils n'existaient pas)
        self.ecrire_resultats_json()
    
    def charger_resultats_depuis_json(self, repertoire_logs: Path):
        """Charge les résultats depuis les fichiers JSON séparés"""
        self.tous_les_resultats = {}
        
        # Chercher tous les fichiers resultats_*.json
        pattern = repertoire_logs / "resultats_*.json"
        fichiers_json = list(repertoire_logs.glob("resultats_*.json"))
        
        if not fichiers_json:
            print(f"Aucun fichier de résultats trouvé dans {repertoire_logs}")
            return
        
        for fichier_json in fichiers_json:
            nom_test = fichier_json.stem.replace("resultats_", "")
            try:
                with open(fichier_json, 'r', encoding='utf-8') as f:
                    resultats = json.load(f)
                    self.tous_les_resultats[nom_test] = resultats
                print(f"Résultats chargés depuis {fichier_json}")
            except Exception as e:
                print(f"Erreur lors du chargement de {fichier_json}: {e}")
        
        # Traiter chaque test avec les résultats chargés
        try:
            for nom_test, config in self.resultats_attendus['tests'].items():
                attendus = config['resultats']
                obtenus = self.tous_les_resultats.get(nom_test, {})
                
                if not obtenus or 'erreur' in obtenus:
                    self.rapport.append((nom_test, "ERREUR", 0))
                    self.details_tests.append({
                        'nom': nom_test,
                        'points': 0,
                        'points_max': config.get('points_max', 15)
                    })
                    continue
                
                points, points_max = self.comparer_resultats(nom_test, obtenus, attendus)
                self.note_totale += points
                
                # Test de stabilité si configuré
                bonus_stabilite = 0
                if config.get('test_stabilite', False):
                    nb_reps = config.get('nb_repetitions', 10)
                    stable = self.tester_repetitions(nom_test, config['fichier_csv'], nb_reps)
                    if stable:
                        print(f"  Bonus stabilite: +2 points")
                        bonus_stabilite = 2
                        self.note_totale += 2
                    else:
                        print(f"  Penalite instabilite: -2 points")
                        bonus_stabilite = -2
                        self.note_totale -= 2
                
                self.details_tests.append({
                    'nom': nom_test,
                    'points': points + bonus_stabilite,
                    'points_max': points_max + (2 if config.get('test_stabilite', False) else 0)
                })
                
                self.rapport.append((nom_test, "COMPLETE", points + bonus_stabilite))
                
        except Exception as e:
            print(f"Erreur lors du chargement du JSON: {e}")
            print("Basculement vers exécution complète des tests...")
            self.executer_tests_complets()
    
    def executer_tests_complets(self):
        """Exécute tous les tests complètement (version originale)"""
        for nom_test, config in self.resultats_attendus['tests'].items():
            fichier_csv = config['fichier_csv']
            attendus = config['resultats']
            
            obtenus = self.executer_test(nom_test, fichier_csv)
            self.tous_les_resultats[nom_test] = obtenus
            
            if 'erreur' in obtenus:
                self.rapport.append((nom_test, "ERREUR", 0))
                self.details_tests.append({
                    'nom': nom_test,
                    'points': 0,
                    'points_max': config.get('points_max', 15)
                })
                continue
            
            points, points_max = self.comparer_resultats(nom_test, obtenus, attendus)
            self.note_totale += points
            
            bonus_stabilite = 0
            if config.get('test_stabilite', False):
                nb_reps = config.get('nb_repetitions', 10)
                stable = self.tester_repetitions(nom_test, fichier_csv, nb_reps)
                if stable:
                    print(f"  Bonus stabilite: +2 points")
                    bonus_stabilite = 2
                    self.note_totale += 2
                else:
                    print(f"  Pénalité instabilite: -2 points")
                    bonus_stabilite = -2
                    self.note_totale -= 2
            
            self.details_tests.append({
                'nom': nom_test,
                'points': points + bonus_stabilite,
                'points_max': points_max + (2 if config.get('test_stabilite', False) else 0)
            })
            
            self.rapport.append((nom_test, "COMPLETE", points + bonus_stabilite))
        """Écrit tous les résultats obtenus dans un fichier JSON"""
        fichier_json = self.repertoire / "logs" / "resultats_obtenus.json"
        fichier_json.parent.mkdir(parents=True, exist_ok=True)
        
        with open(fichier_json, 'w', encoding='utf-8') as f:
            json.dump(self.tous_les_resultats, f, indent=2, ensure_ascii=False)
        
        print(f"\nRésultats écrits dans: {fichier_json}")
    
    def generer_rapport_final(self):
        """Génère le rapport final"""
        print("\n" + "="*70)
        print("RAPPORT FINAL")
        print("="*70)
        
        print("\nDetails par test:")
        print(f"  {'Compilation':30s} {self.compilation_points:2d}/{self.compilation_points:2d} points")
        
        for detail in self.details_tests:
            points = detail['points']
            points_max = detail['points_max']
            print(f"  {detail['nom']:30s} {points:2d}/{points_max:2d} points")
        
        print(f"\n{'='*70}")
        print(f"NOTE FINALE: {self.note_totale}/{self.note_max}")
        print(f"{'='*70}\n")
        
        rapport_json = {
            'note': self.note_totale,
            'note_max': self.note_max,
            'details': [
                {'test': nom, 'statut': statut, 'points': points}
                for nom, statut, points in self.rapport
            ]
        }
        
        fichier_rapport = self.repertoire / "evaluation" / "rapport_correction.json"
        fichier_rapport.parent.mkdir(exist_ok=True)
        with open(fichier_rapport, 'w') as f:
            json.dump(rapport_json, f, indent=2)
        
        print(f"Rapport sauvegarde dans: {fichier_rapport}")
    
    def run_only(self):
        """Exécute seulement les tests et génère le fichier JSON"""
        print("\n" + "="*70)
        print("EXECUTION DES TESTS - TP1 IFT630")
        print(f"Repertoire: {self.repertoire}")
        print("="*70)
        
        if not self.compiler_projet():
            print("Erreur de compilation")
            return
        
        self.executer_tous_les_tests()
        self.ecrire_resultats_json()
        
        # Afficher les résultats sur la console
        print("\n" + "="*50)
        print("RESULTATS OBTENUS")
        print("="*50)
        for scenario, resultats in self.tous_les_resultats.items():
            print(f"\n{scenario.upper()}:")
            for cle, valeur in resultats.items():
                print(f"  {cle}: {valeur}")
    
    def corriger(self):
        """Exécute la correction complète"""
        print("\n" + "="*70)
        print("CORRECTION AUTOMATIQUE - TP1 IFT630")
        print(f"Repertoire: {self.repertoire}")
        print("="*70)
        
        if not self.compiler_projet():
            self.generer_rapport_final()
            return
        
        self.executer_tous_les_tests()
        self.generer_rapport_final()
    
    def ecrire_resultats_json(self):
        """Écrit tous les résultats obtenus dans un fichier JSON"""
        fichier_json = self.repertoire / "logs" / "resultats_obtenus.json"
        fichier_json.parent.mkdir(parents=True, exist_ok=True)
        
        with open(fichier_json, 'w', encoding='utf-8') as f:
            json.dump(self.tous_les_resultats, f, indent=2, ensure_ascii=False)
        
        print(f"\nRésultats écrits dans: {fichier_json}")


def main():
    if len(sys.argv) < 2:
        print("Usage: python3 corriger_tp1.py <repertoire> [--run-only]")
        sys.exit(1)
    
    repertoire = sys.argv[1]
    run_only = "--run-only" in sys.argv
    
    if not os.path.exists(repertoire):
        print(f"ERREUR: Le repertoire {repertoire} n'existe pas")
        sys.exit(1)
    
    correcteur = CorrecteurTP1(repertoire)
    if run_only:
        correcteur.run_only()
    else:
        correcteur.corriger()


if __name__ == "__main__":
    main()