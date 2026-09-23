"""Public recovery check. Loops here are provided grading infrastructure.

The exercise derives array weights explicitly. Loops, comprehensions, recursion,
loop wrappers, generic linear solvers/inverses, and signal-filter shortcuts do not
meet that learning objective. These source checks catch ordinary usage; they are
not a security sandbox or a complete proof of compliance.
"""
import argparse
import ast
import importlib.util
import inspect
from pathlib import Path
import textwrap

import numpy as np

LOOP_WRAPPERS = {"map", "vectorize", "apply_along_axis", "frompyfunc"}
SOLVERS = {"solve", "solve_triangular", "solve_banded", "solveh_banded",
           "inv", "lstsq", "pinv", "pinvh", "tensorinv", "tensorsolve"}
FILTERS = {"lfilter", "filtfilt", "sosfilt", "sosfiltfilt", "deconvolve"}


def code_violations(tree):
    """Check syntax plus ordinary imported/assigned aliases and call cycles."""
    messages = set()
    aliases = {}
    for node in ast.walk(tree):
        if isinstance(node, ast.ImportFrom):
            for name in node.names:
                aliases[name.asname or name.name] = name.name
        elif isinstance(node, ast.Import):
            for name in node.names:
                aliases[name.asname or name.name] = name.name

    def call_name(node):
        if isinstance(node, ast.Attribute):
            return node.attr
        if isinstance(node, ast.Name):
            name = node.id
            seen = set()
            while name in aliases and name not in seen:
                seen.add(name)
                name = aliases[name]
            return name
        return ""

    assignments = [node for node in ast.walk(tree) if isinstance(node, ast.Assign)]
    for _ in range(len(assignments) + 1):
        for node in assignments:
            name = call_name(node.value)
            if name:
                for target in node.targets:
                    if isinstance(target, ast.Name):
                        aliases[target.id] = name
    loops = (ast.For, ast.AsyncFor, ast.While, ast.ListComp, ast.SetComp,
             ast.DictComp, ast.GeneratorExp)
    definitions = {node.name: node for node in ast.walk(tree)
                   if isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef))}
    graph = {name: set() for name in definitions}
    for node in ast.walk(tree):
        if isinstance(node, loops):
            messages.add("Use array operations, not loops or comprehensions.")
        if isinstance(node, ast.Call):
            name = call_name(node.func)
            if name in LOOP_WRAPPERS:
                messages.add(name + " is not allowed as a loop substitute.")
            if name in SOLVERS | FILTERS:
                messages.add("Derive and build the recovery weights; " + name + " bypasses this exercise.")
    for name, definition in definitions.items():
        graph[name] = {call_name(node.func) for node in ast.walk(definition)
                       if isinstance(node, ast.Call)} & definitions.keys()
    def cycle(name, path):
        if name in path:
            return True
        return any(cycle(child, path | {name}) for child in graph[name])
    if any(cycle(name, set()) for name in graph):
        messages.add("Use array operations, not recursion.")
    return sorted(messages)


def source_violations(function):
    try:
        return code_violations(ast.parse(textwrap.dedent(inspect.getsource(function))))
    except (OSError, TypeError):
        return None


def _cases():
    # Independent oracle: start with true readings and apply the FORWARD fault.
    # Do not duplicate the inverse matrix students are expected to discover.
    rng = np.random.default_rng(63019)
    signals = [
        ("paper example", np.array([9, 6, 3, 0, 6, 3], dtype=float)),
        ("one reading", np.array([-2.5])),
        ("two readings", np.array([3., -4.])),
        ("zero readings", np.zeros(11)),
        ("signed and fractional readings", np.array([.25, -1.5, 0., 2.75, -4., .125, 3.])),
        ("initial impulse", np.r_[6., np.zeros(18)]),
        ("final impulse", np.r_[np.zeros(18), -3.]),
        ("odd length", rng.normal(size=17)),
        ("maximum length", rng.normal(size=64)),
    ]
    for name, truth in signals:
        recorded = truth.copy()
        recorded[1:] += truth[:-1] / 3.0
        yield name, recorded, truth
    yield "integer input", np.array([9, 9, 5, 1, 6, 5]), np.array([9., 6., 3., 0., 6., 3.])
    # A view ensures implementations do not silently assume contiguous inputs.
    backing = rng.normal(size=26)
    truth = backing[::2].copy()
    recorded = backing[::2]
    recorded[1:] += truth[:-1] / 3.0
    yield "noncontiguous input", recorded, truth


def check_recovery(function, verbose=True):
    """One pass/fail check for recover_readings(recorded), for lengths 1..64."""
    failures = []
    syntax = source_violations(function)
    if syntax:
        failures.extend(syntax)
    for name, recorded, expected in _cases():
        before = recorded.copy()
        try:
            actual = function(recorded)
            np.testing.assert_array_equal(recorded, before, err_msg="Do not mutate the recorded input.")
            assert isinstance(actual, np.ndarray), "Return a NumPy array."
            assert actual.shape == expected.shape, f"Expected shape {expected.shape}; got {actual.shape}."
            assert np.issubdtype(actual.dtype, np.floating), "Return a floating-point array."
            assert np.isfinite(actual).all(), "Return finite readings."
            np.testing.assert_allclose(actual, expected, rtol=1e-10, atol=1e-12,
                                       err_msg="Recovered readings do not match the original signal.")
        except Exception as exc:
            failures.append(f"{name}: {type(exc).__name__}: {exc}")
    if verbose:
        print(("FAIL" if failures else "PASS") + " - recover the original sensor readings")
        for failure in failures:
            print("  " + failure)
        if syntax is None:
            print("Source unavailable here: array-operation compliance needs manual review.")
    return not failures


def load_file(path):
    spec = importlib.util.spec_from_file_location("submitted_functions", Path(path).resolve())
    if spec is None or spec.loader is None:
        raise ValueError("Cannot load Python file: " + str(path))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("file", type=Path)
    args = parser.parse_args()
    passed = check_recovery(load_file(args.file).recover_readings)
    print("Local feedback only; no files were submitted or uploaded.")
    return 0 if passed else 1


if __name__ == "__main__":
    raise SystemExit(main())
