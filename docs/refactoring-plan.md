# Refactoring Plan: Raw Data Import

## Ziel
Den Import so umbauen, dass **alle** Daten aus `packs/pf2e` importiert werden und die Struktur korrekt die Rohdaten-Natur widerspiegelt.

## Änderungen

### 1. Entity umbenennen: `PathfinderItem` → `FoundryRawEntry`

**Begründung:** Es handelt sich um Rohdaten aus dem Foundry-Repo, nicht um fertig verarbeitete Items.

**Änderungen:**
- `PathfinderItem.kt` → `FoundryRawEntry.kt`
- `PathfinderItemRepository.kt` → `FoundryRawEntryRepository.kt`
- Tabelle: `pathfinder_items` → `foundry_raw_entries`
- Neue Flyway-Migration für Tabellen-Umbenennung

### 2. ItemType-Enum entfernen, durch String ersetzen

**Begründung:** Der `type` kommt direkt aus dem JSON und ist variabel (spell, feat, action, ancestry, heritage, equipment, npc, hazard, vehicle, ...). Ein Enum ist zu restriktiv.

**Änderungen:**
- `itemType: ItemType` → `foundryType: String`
- Wert aus JSON: `jsonNode.get("type")?.asText()`
- Index bleibt erhalten für Queries

### 3. Import-Service generalisieren

**Aktuell:**
```kotlin
fun importFeats(jobId: UUID?) = importItems("$PATH_PREFIX/feats", ItemType.FEAT, jobId)
fun importSpells(jobId: UUID?) = importItems("$PATH_PREFIX/spells", ItemType.SPELL, jobId)
```

**Neu:**
```kotlin
fun importAll(jobId: UUID?): ImportResult
fun importCategory(category: String, jobId: UUID?): ImportResult  // z.B. "feats", "spells", "actions"
```

**Änderungen:**
- `PathfinderImportService` → `FoundryImportService`
- Methode `importAll()` iteriert über alle Kategorien
- Methode `importCategory(category)` für einzelne Kategorien
- `processEntry()` liest `type` aus JSON statt Parameter

### 4. Controller anpassen

**Aktuell:**
```
POST /api/import/feats
POST /api/import/spells
```

**Neu:**
```
POST /api/import/all              # Importiert alles
POST /api/import/{category}       # z.B. /api/import/feats, /api/import/actions
GET  /api/import/categories       # Liste aller verfügbaren Kategorien
```

### 5. Datenbank-Migration

Neue Flyway-Migration `V2__rename_to_foundry_raw_entries.sql`:
```sql
-- Tabelle umbenennen
ALTER TABLE pathfinder_items RENAME TO foundry_raw_entries;

-- Spalte umbenennen und Typ ändern
ALTER TABLE foundry_raw_entries RENAME COLUMN item_type TO foundry_type;
ALTER TABLE foundry_raw_entries ALTER COLUMN foundry_type TYPE VARCHAR(64);

-- Spalte umbenennen
ALTER TABLE foundry_raw_entries RENAME COLUMN item_name TO name;

-- Indizes aktualisieren
ALTER INDEX idx_item_type RENAME TO idx_foundry_type;
```

## Dateiänderungen

| Alte Datei | Neue Datei |
|------------|------------|
| `domain/PathfinderItem.kt` | `domain/FoundryRawEntry.kt` |
| `domain/PathfinderItemRepository.kt` | `domain/FoundryRawEntryRepository.kt` |
| `importer/PathfinderImportService.kt` | `importer/FoundryImportService.kt` |
| `web/ImportController.kt` | `web/ImportController.kt` (anpassen) |

## Reihenfolge

1. Flyway-Migration erstellen (V2)
2. Entity + Repository umbenennen und anpassen
3. Import-Service refactoren
4. Controller anpassen
5. Tests aktualisieren
6. Vollständigen Import testen

## Ergebnis

Nach dem Refactoring:
- Ein Aufruf `POST /api/import/all` importiert **alle** ~110 Kategorien
- Der `foundryType` wird korrekt aus dem JSON gelesen
- Die Naming spiegelt wider, dass es Rohdaten für spätere Vektorisierung sind
