// ============================================================
// init-fse-db.js
// Script di inizializzazione MongoDB per il Gateway FSE 2.0
//
// Esegue automaticamente all'avvio del container Docker e:
//  1. Ricrea la collezione "dictionary" dai sistemi in "terminology"
//  2. Verifica che "engines" sia popolata
//
// Esecuzione manuale:
//   docker exec gtw-lite-mongo-1 mongo FSE_GTW /docker-entrypoint-initdb.d/init-fse-db.js
// ============================================================

var db = db.getSiblingDB('FSE_GTW');

// ============================================================
// 1. DICTIONARY
// Ricrea la collezione dictionary dai sistemi distinti in terminology
// ============================================================
print('>>> Verifica collezione dictionary...');

var dictionaryCount = db.dictionary.count({deleted: false});

if (dictionaryCount === 0) {
    print('>>> Dictionary vuota, popolamento in corso...');

    var systems = db.terminology.distinct('system');
    var inserted = 0;

    systems.forEach(function(system) {
        var doc = db.terminology.findOne({system: system});
        if (doc) {
            db.dictionary.insertOne({
                system: system,
                version: doc.version,
                whitelist: false,
                release_date: new Date(),
                creation_date: new Date(),
                deleted: false
            });
            inserted++;
        }
    });

    print('>>> Dictionary popolata con ' + inserted + ' sistemi.');
} else {
    print('>>> Dictionary già presente con ' + dictionaryCount + ' sistemi. Skip.');
}

// ============================================================
// 2. ENGINES
// Verifica che la collezione engines sia popolata
// ============================================================
print('>>> Verifica collezione engines...');

var enginesCount = db.engines.count();

if (enginesCount === 0) {
    print('>>> ATTENZIONE: La collezione engines è vuota!');
    print('>>> Eseguire manualmente:');
    print('>>> docker exec gtw-lite-mongo-1 mongoimport --db FSE_GTW --collection engines --file /var/lib/mongo/data/engines.json --jsonArray');
} else {
    print('>>> Engines presente con ' + enginesCount + ' documento/i. OK.');
}

// ============================================================
// 3. TERMINOLOGY — verifica campo deleted
// ============================================================
print('>>> Verifica campo deleted in terminology...');

var terminologyWithoutDeleted = db.terminology.count({deleted: {$exists: false}});

if (terminologyWithoutDeleted > 0) {
    print('>>> Aggiornamento campo deleted in terminology (' + terminologyWithoutDeleted + ' documenti)...');
    db.terminology.updateMany(
        {deleted: {$exists: false}},
        {$set: {deleted: false}}
    );
    print('>>> Terminology aggiornata.');
} else {
    print('>>> Terminology OK, campo deleted già presente.');
}

print('>>> Inizializzazione FSE_GTW completata.');