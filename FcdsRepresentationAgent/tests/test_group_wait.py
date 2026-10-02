import importlib.util
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('group_wait', ROOT/'ui/patch_group_wait.py')
module = importlib.util.module_from_spec(spec);spec.loader.exec_module(module)

class GroupWaitTests(unittest.TestCase):
    def test_scoped_copy_and_idempotence(self):
        source = '''            // Open window immediately to avoid popup blocker
<h2>Starting JupyterLab...</h2><p>This may take up to 5 minutes</p>
showStatus('⏳ Starting JupyterLab... This may take up to 5 minutes.', 'loading');'''
        result = module.patch(source)
        self.assertIn("course === 'fcds-p2-26-fall-1a'", result)
        self.assertIn('group of 1–3 students', result)
        self.assertIn("? 'Forming your group and starting JupyterLab...' : 'Starting JupyterLab...'", result)
        self.assertEqual(module.patch(result), result)
