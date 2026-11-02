"""Exercise actual history SQL with SQLite. Does not compile Kotlin or emulate Android."""
from pathlib import Path
import re
import sqlite3
import unittest

source = (Path(__file__).resolve().parents[1] / 'app/src/main/java/com/callcapture/app/data/messages/MessageHistoryStore.kt').read_text()
def constant(name):
    return re.search(r'const val '+name+r' = "([^"]+)"', source).group(1)

class HistoryDatabaseTests(unittest.TestCase):
    def setUp(self):
        self.db = sqlite3.connect(':memory:')
        self.db.execute(constant('CREATE_SQL'))
        self.db.execute(re.search(r'db.execSQL\("(CREATE TABLE deleted[^\"]+)"\)', source).group(1))
    def tearDown(self):
        self.db.close()
    def insert(self, ident, received=100):
        self.db.execute('INSERT OR IGNORE INTO history VALUES(?,?,?,?,?,?,?)',
            (ident, 'com.whatsapp', 'Chat', 'Sender', 'Message text', 50, received))
    def test_reposted_identity_is_not_duplicated(self):
        self.insert('same'); self.insert('same')
        self.assertEqual(self.db.execute('SELECT count(*) FROM history').fetchone()[0], 1)
    def test_same_body_with_distinct_message_identity_is_kept(self):
        self.insert('first'); self.insert('second')
        self.assertEqual(self.db.execute('SELECT count(*) FROM history').fetchone()[0], 2)
    def test_cap_keeps_latest_entries(self):
        for i in range(5002): self.insert(str(i), i)
        self.db.execute(constant('CAP_SQL'))
        count, oldest = self.db.execute('SELECT count(*), min(received) FROM history').fetchone()
        self.assertEqual((count, oldest), (5000, 2))
    def test_retention_boundary(self):
        self.insert('expired', 99); self.insert('boundary', 100); self.insert('new', 101)
        self.db.execute('DELETE FROM history WHERE received < ?', (100,))
        self.assertEqual({r[0] for r in self.db.execute('SELECT id FROM history')}, {'boundary','new'})
    def test_individual_delete_leaves_no_text_but_retains_dedup_marker(self):
        self.insert('one')
        self.db.execute('INSERT OR IGNORE INTO deleted VALUES(?,?)', ('one', 200))
        self.db.execute('DELETE FROM history WHERE id = ?', ('one',))
        self.assertEqual(self.db.execute('SELECT count(*) FROM history').fetchone()[0], 0)
        self.assertEqual(self.db.execute('SELECT 1 FROM deleted WHERE id = ?', ('one',)).fetchone(), (1,))
    def test_bulk_clear_removes_history_and_replay_identifiers(self):
        self.insert('one')
        self.db.execute('INSERT INTO deleted VALUES(?,?)', ('removed-message', 200))
        clear_body = source.split('@Synchronized fun clear()', 1)[1].split('data class Entry', 1)[0]
        tables = re.findall(r'db.delete\("([^"]+)", null, null\)', clear_body)
        self.assertEqual(set(tables), {'history', 'deleted'})
        for table in tables:
            self.db.execute('DELETE FROM ' + table)
        self.assertEqual(self.db.execute('SELECT count(*) FROM history').fetchone()[0], 0)
        self.assertEqual(self.db.execute('SELECT count(*) FROM deleted').fetchone()[0], 0)

    def test_clear_is_isolated_from_other_databases(self):
        self.insert('one')
        self.db.execute('DELETE FROM history')
        self.assertEqual(self.db.execute('SELECT count(*) FROM history').fetchone()[0], 0)
        # The store contains no recording paths, tables, or call-state writes.
        self.assertNotIn('CallRecordingService', source)
        self.assertNotIn('recordings/', source)

if __name__ == '__main__': unittest.main(verbosity=2)
