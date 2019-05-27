# Readme

## Projet éclipse 

Ouvrable et exécutable avec Eclipse Luna (<a href="https://www.eclipse.org/downloads/packages/release/luna/sr2">Téléchargement</a>).

 Les librairies additionnelles suivantes sont nécessaires (disponibles dans la racine du projet) : 

- commons-cli-1.3.1.jar
- org.eclipse.emf_2.6.0.jar

Pour l'exécution, le paramètre `-p` (`--path`, le chemin vers le fichier .scxml) est obligatoire. 

## Code source

Disponible sur <a href="https://github.com/nicotitine/projet-tutore">GitHub</a>.

## Balises traitées

- `<scxml>`
- `<state>`
- `<parallel>`
- `<transition>`
- `<final>`
- `<history`>
- `<datamodel>`
- `<data>`

## Balises manquantes 

Les balises liées au comportement, principalement des états. Ces balises (`<onentry>`, `<onexit>`, `<assign>`, `<send>` et `<cancel`>)  correspondent à la partie entourée en rouge ci-dessous.

![](C:\Users\nhinc\Desktop\Sans titre 3.jpg)

Nous n'avons pas été capables de paramétrer correctement les comportements en fonction du contenu SCXML