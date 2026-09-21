"""Shared assessment contract for session checks and Bree final grading."""
import ast
import contextlib
import io
import json
from pathlib import Path

from public_checks import check_part_a, check_part_b

TASKS = {
    "task1": ("build_comparisons", check_part_a),
    "task2": ("choose_and_summarize", check_part_b),
}


def extract_code(notebook, task):
    if task not in TASKS:
        raise ValueError("Unknown task: " + task)
    return "\n".join(
        "".join(cell.get("source", []))
        for cell in notebook.get("cells", [])
        if cell.get("cell_type") == "code"
        and task in cell.get("metadata", {}).get("tags", [])
    )


def assess(task, code):
    """Return (passed, feedback); enforce the assignment's array-operation rule."""
    if task not in TASKS:
        return False, "Unknown task: " + task
    if not code.strip():
        return False, "No solution cells found. Keep the supplied task tags."
    try:
        tree = ast.parse(code)
        loops = (ast.For, ast.AsyncFor, ast.While, ast.ListComp, ast.SetComp,
                 ast.DictComp, ast.GeneratorExp)
        forbidden = {"map", "vectorize", "apply_along_axis", "frompyfunc"}
        for node in ast.walk(tree):
            if isinstance(node, loops):
                return False, "Use array operations, not loops or comprehensions."
            if isinstance(node, ast.Call):
                name = getattr(node.func, "attr", getattr(node.func, "id", ""))
                if name in forbidden:
                    return False, name + " is not allowed as a loop substitute."
        namespace = {}
        output = io.StringIO()
        with contextlib.redirect_stdout(output), contextlib.redirect_stderr(output):
            exec(compile(tree, "<student-solution>", "exec"), namespace)
            name, checker = TASKS[task]
            function = namespace.get(name)
            if not callable(function):
                return False, "Define " + name + " with the supplied signature."
            passed = checker(function)
        # The submitted source has already been checked above, even if inspect
        # cannot recover source for a function created with exec.
        feedback = output.getvalue().replace(
            "Source unavailable here: no-loop compliance needs manual review.\n", "")
        return bool(passed), feedback.strip()
    except Exception as exc:
        return False, type(exc).__name__ + ": " + str(exc)


if __name__ == "__main__":
    import sys
    request = json.loads(sys.stdin.read())
    passed, feedback = assess(request["task"], request["code"])
    print(json.dumps({"passed": passed, "feedback": feedback}))
