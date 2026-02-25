# ⚔️ FazioniCore

> Plugin fazioni professionale per Paper 1.21.1 — economia, claim, guerre e molto altro.

![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square)
![Paper](https://img.shields.io/badge/Paper-1.21.1-blue?style=flat-square)
![Vault](https://img.shields.io/badge/Vault-dipendenza-green?style=flat-square)
![Classi](https://img.shields.io/badge/Classi-18%2F20-lightgrey?style=flat-square)

---

## 📋 Indice

- [Requisiti](#-requisiti)
- [Installazione](#-installazione)
- [Compilazione](#-compilazione)
- [Comandi](#-comandi)
- [Permessi](#-permessi)
- [Funzionalità](#-funzionalità)
- [Configurazione](#-configurazione)
- [Database](#-database-mysql)
- [Struttura progetto](#-struttura-progetto)

---

## 🔧 Requisiti

| Requisito | Versione |
|---|---|
| Server | Paper 1.21.1 |
| Java | 21+ |
| Vault | Qualsiasi versione recente |
| Plugin economia | EssentialsX, CMI, ecc. (compatibile Vault) |
| MySQL *(opzionale)* | 8.0+ |

---

## 📦 Installazione

1. Scarica il jar compilato e inseriscilo nella cartella `plugins/`
2. Assicurati che **Vault** e un plugin economia siano installati
3. Avvia il server — la configurazione viene generata automaticamente in `plugins/FazioniCore/`
4. Modifica `config.yml` secondo le tue esigenze
5. Ricarica con `/f reload` (oppure riavvia il server)

---

## 🔨 Compilazione

```bash
git clone <repo>
cd FazioniCore
mvn clean package
```

Il jar finale si trova in `target/FazioniCore-1.0.0.jar`.

---

## 💬 Comandi

Tutti i comandi sono accessibili tramite `/faction`, `/fazione` o `/f`.

### Gestione Fazione

| Comando | Descrizione | Ruolo minimo |
|---|---|---|
| `/f create <nome> <tag>` | Crea una nuova fazione (ha un costo) | — |
| `/f disband` | Scioglie la fazione | Leader |
| `/f info [fazione]` | Mostra informazioni sulla fazione | Tutti |
| `/f list` | Lista di tutte le fazioni | Tutti |
| `/f desc <testo>` | Imposta la descrizione della fazione | Officer |
| `/f tag <tag>` | Cambia il tag della fazione | Leader |
| `/f ff` | Attiva/disattiva il friendly fire | Leader |
| `/f open` | Rende la fazione aperta o su invito | Leader |

### Membri

| Comando | Descrizione | Ruolo minimo |
|---|---|---|
| `/f invite <giocatore>` | Invita un giocatore | Officer |
| `/f join <fazione>` | Entra in una fazione (su invito o se aperta) | — |
| `/f leave` | Lascia la fazione | Tutti |
| `/f kick <giocatore>` | Espelle un membro | Officer |
| `/f promote <giocatore>` | Promuove un membro di un rango | Leader |
| `/f demote <giocatore>` | Retrocede un membro di un rango | Leader |

### Territori

| Comando | Descrizione | Ruolo minimo |
|---|---|---|
| `/f claim` | Claima il chunk in cui ti trovi | Officer |
| `/f unclaim` | Rimuove il claim del chunk corrente | Officer |
| `/f map` | Mostra la mappa dei chunk circostanti | Tutti |

### Economia

| Comando | Descrizione | Ruolo minimo |
|---|---|---|
| `/f bank` | Mostra il saldo della banca | Tutti |
| `/f bank deposit <importo>` | Deposita nella banca della fazione | Tutti |
| `/f bank withdraw <importo>` | Preleva dalla banca della fazione | Officer |

### Guerra

| Comando | Descrizione | Ruolo minimo |
|---|---|---|
| `/f war <fazione>` | Dichiara guerra a una fazione | Leader |
| `/f endwar` | Termina la guerra attiva | Leader |

### Shield

| Comando | Descrizione | Ruolo minimo |
|---|---|---|
| `/f shield` | Attiva/disattiva lo shield | Leader |
| `/f shield start <HH:mm>` | Imposta l'orario di inizio shield | Leader |
| `/f shield end <HH:mm>` | Imposta l'orario di fine shield | Leader |

### Admin

| Comando | Descrizione |
|---|---|
| `/f reload` | Ricarica la configurazione | 

---

## 🔑 Permessi

| Permesso | Descrizione | Default |
|---|---|---|
| `fazionicore.use` | Permesso base per usare i comandi | `true` |
| `fazionicore.admin` | Accesso ai comandi admin + bypass protezioni | `op` |

---

## ✨ Funzionalità

### Sistema Ruoli
I membri di una fazione hanno uno dei seguenti ruoli, in ordine crescente di autorità:

```
RECRUIT → MEMBER → OFFICER → LEADER
```

Ogni ruolo eredita i permessi del precedente. Il leader può trasferire la propria posizione promuovendo un Officer.

### Sistema Livelli
Il livello della fazione viene calcolato automaticamente in base a:
```
Punteggio = (claim totali effettuati) + (numero membri × moltiplicatore)
```
I livelli sbloccano **più slot claim**, **più membri** e un **bonus interesse** sulla banca. Tutto configurabile in `config.yml`.

### Sistema Claim
- I chunk si claimano con `/f claim` con un costo in valuta
- Il limite di claim attivo dipende dal livello della fazione
- Nei territori claimati è vietato costruire/rompere a chi non appartiene alla fazione
- I claim vengono visualizzati con `/f map` (`+` posizione attuale, `#` claim)

### Sistema Guerra
- Una fazione può dichiarare guerra a un'altra con `/f war`
- Durante la guerra i territori nemici sono accessibili per il raid
- Ogni blocco rotto e ogni kill guadagnano **punti guerra**
- Al termine della guerra, il vincitore (più punti) riceve una **ricompensa economica** nella banca
- Sono presenti cooldown configurabili tra un raid e l'altro e tra dichiarazioni di guerra

### Shield
- Ogni fazione può impostare un **intervallo orario** in cui il territorio è inviolabile
- Supporta orari a cavallo della mezzanotte (es. 22:00 → 08:00)
- Durante lo shield nessuno, nemmeno una fazione in guerra, può rompere blocchi nel territorio

### Anti-Inside
- Un membro appena entrato in fazione non può interagire con i blocchi nel proprio territorio per un tempo configurabile
- Previene il classico exploit di entrare in una fazione per sabotarla dall'interno

### Raid Log
- Ogni blocco rotto in territorio nemico durante una guerra viene registrato
- Il log include: giocatore, UUID, tipo blocco, coordinate, timestamp
- Con MySQL abilitato i log sono su DB; altrimenti su file `.log` per fazione

### Banca Fazione
- Ogni membro può depositare valuta nella banca della fazione
- Solo Officer e superiori possono prelevare
- È possibile applicare una tassa percentuale sui prelievi
- I livelli superiori possono garantire un bonus di interesse

---

## ⚙️ Configurazione

Il file `config.yml` viene generato automaticamente al primo avvio. Di seguito le sezioni principali:

```yaml
faction:
  creation-cost: 500.0       # Costo creazione fazione
  max-name-length: 16
  friendly-fire-default: false

claim:
  cost: 100.0                # Costo per claim
  enabled-worlds:
    - world

levels:
  thresholds:
    2: 15                    # Punteggio minimo per livello 2
    3: 35
  bonuses:
    max-members:
      1: 10                  # Lv1 → max 10 membri
      2: 20

war:
  tnt-damage-in-claims: true
  creeper-damage-in-claims: false
  raid-cooldown-hours: 6
  war-points-to-money: 10.0  # 1 punto = 10 monete

anti-inside:
  enabled: true
  protection-time-minutes: 30

shield:
  enabled: true
  default-start: "22:00"
  default-end: "10:00"
```

---

## 🗄️ Database MySQL

Di default FazioniCore usa file YAML per salvare fazioni, claim e guerre. Per abilitare MySQL:

```yaml
database:
  enabled: true
  host: localhost
  port: 3306
  name: fazionicore
  username: root
  password: password
```

Con MySQL abilitato, i **raid log** vengono salvati nella tabella `raid_logs`. Fazioni, claim e guerre continuano a usare YAML per massima portabilità. Se la connessione MySQL fallisce, il plugin fa automaticamente fallback su YAML senza interrompere il funzionamento.

---

## 🏗️ Struttura Progetto

```
FazioniCore/
├── pom.xml
└── src/main/
    ├── java/it/fazionicore/
    │   ├── FazioniCore.java            ← Entrypoint plugin
    │   ├── ConfigManager.java          ← Lettura config.yml
    │   ├── DatabaseManager.java        ← MySQL + YAML fallback
    │   ├── EconomyManager.java         ← Wrapper Vault
    │   ├── Messages.java               ← Messaggi colorati
    │   ├── command/
    │   │   └── FactionCommand.java     ← /f + tutti i subcommand
    │   ├── listener/
    │   │   ├── ProtectionListener.java ← Claim, esplosioni, PvP
    │   │   └── PlayerListener.java     ← Join/quit
    │   ├── manager/
    │   │   ├── FactionManager.java
    │   │   ├── ClaimManager.java
    │   │   ├── WarManager.java
    │   │   ├── ShieldManager.java
    │   │   └── AntiInsideManager.java
    │   └── model/
    │       ├── Faction.java
    │       ├── FactionRole.java
    │       ├── ClaimData.java
    │       ├── WarData.java
    │       └── RaidLog.java
    └── resources/
        ├── plugin.yml
        └── config.yml
```

