from pathlib import Path
import os
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
BAZAAR = ROOT / "bazaar"
JAVA = os.environ.get("JAVA") or shutil.which("java")
JAVAC = os.environ.get("JAVAC") or shutil.which("javac")


class BazaarTests(unittest.TestCase):
    def test_launcher_resolves_main_from_pinned_dependency(self):
        with tempfile.TemporaryDirectory() as folder:
            result = subprocess.run([JAVA, "-Djava.awt.headless=true", "-jar",
                str(BAZAAR / "build/fcdsrepresentationagent.jar"), "--invalid-offline-test-option"],
                cwd=folder, capture_output=True, text=True, timeout=15)
            # Argument rejection happens before any agent/network initialization.
            self.assertNotEqual(result.returncode, 0)
            self.assertIn("UnrecognizedOptionException", result.stderr)
            self.assertNotIn("ClassNotFoundException", result.stderr)

    def test_real_jar_phase_and_camera_contract(self):
        with tempfile.TemporaryDirectory() as folder:
            work = Path(folder)
            shutil.copytree(BAZAAR / "runtime", work, dirs_exist_ok=True)
            for name in ("planstatus", "logs", "chat_history"):
                (work / name).mkdir()
            (work / "capture.key").write_text("offline-test-key-at-least-thirty-two-characters")
            properties = work / "properties"
            shutil.copyfile(properties / "RepresentationPlanExecutor.properties", properties / "TestPlan.properties")
            (properties / "State.properties").write_text("ignore_prefixes=Private_,Camera_,tab_group\n")
            (properties / "PromptStepHandler.properties").write_text("prompt_file=plans/plan_prompts.xml\nwords_per_minute=400\n")
            (properties / "RepresentationCameraListener.properties").write_text(
                "name=OPEBot\nmodel=openai\nopenai.request.url=http://localhost:1\n"
                "openai.prompt.context=Representation test tutor.\nopenai.context.flag=true\n"
                "openai.context.length=20\nopenai.temperature=0.2\nopenai.model.name=test\n"
                "user-poll-rate=3600\nuser-poll-timeout=1\n")
            (properties / "apiKeys.properties").write_text("offline-test-placeholder\n")
            cp = str(BAZAAR / "build/fcdsrepresentationagent.jar") + ":" + str(BAZAAR / "build/llmcamera-runtime.jar")
            built = subprocess.run([JAVAC, "--release", "8", "-Xlint:-options", "-cp", cp,
                "-d", str(work), str(Path(__file__).with_name("RepresentationHarness.java"))], capture_output=True, text=True)
            self.assertEqual(built.returncode, 0, built.stderr)
            result = subprocess.run([JAVA, "-Djava.awt.headless=true", "-Dbasilica2.handsfree=true", "-cp", str(work) + ":" + cp,
                "basilica2.agents.listeners.plan.RepresentationHarness"], cwd=work, capture_output=True, text=True, timeout=25)
            self.assertEqual(result.returncode, 0, (result.stdout + result.stderr)[-12000:])
            self.assertIn("PASS: phase gates", result.stdout)


if __name__ == "__main__":
    unittest.main()
