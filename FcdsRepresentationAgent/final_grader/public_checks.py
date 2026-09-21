"""Public checks. Test loops are PROVIDED infrastructure, not student code."""

import argparse
import ast
import importlib.util
import inspect
from pathlib import Path
import textwrap

import numpy as np


def _array(actual, expected, kind):
    assert isinstance(actual, np.ndarray), "Return a NumPy array, not a list or scalar."
    expected = np.asarray(expected)
    assert actual.shape == expected.shape, f"Expected shape {expected.shape}; got {actual.shape}."
    if kind == "bool":
        assert actual.dtype == np.dtype(bool), "Eligibility must have Boolean dtype."
    elif kind == "integer":
        assert np.issubdtype(actual.dtype, np.integer), "Choices must have integer dtype."
    else:
        assert np.issubdtype(actual.dtype, np.floating), "Costs and means must be floating-point arrays."
        assert np.isfinite(actual).all(), "Returned costs/means must be finite."
    if kind == "float":
        np.testing.assert_allclose(actual, expected, rtol=1e-10, atol=1e-12)
    else:
        np.testing.assert_array_equal(actual, expected)


def _call(function, inputs, count):
    arrays = tuple(np.array(value, copy=True) for value in inputs)
    snapshots = tuple(value.copy() for value in arrays)
    result = function(*arrays)
    for value, before in zip(arrays, snapshots):
        np.testing.assert_array_equal(value, before, err_msg="Do not mutate input arrays.")
    assert isinstance(result, tuple) and len(result) == count, f"Return a tuple of {count} arrays."
    return result


def source_violations(function):
    """Catch common forbidden syntax; not a complete verifier or security sandbox."""
    try:
        tree = ast.parse(textwrap.dedent(inspect.getsource(function)))
    except (OSError, TypeError):
        return None  # Source can be unavailable in some notebook kernels.
    messages = []
    loop_nodes = (ast.For, ast.AsyncFor, ast.While, ast.ListComp, ast.SetComp,
                  ast.DictComp, ast.GeneratorExp)
    for node in ast.walk(tree):
        if isinstance(node, loop_nodes):
            messages.append("Use whole-array operations, not loops or comprehensions.")
        if isinstance(node, ast.Call):
            name = getattr(node.func, "attr", getattr(node.func, "id", ""))
            if name in {"map", "vectorize", "apply_along_axis", "frompyfunc"}:
                messages.append(f"{name} is not allowed as a loop substitute.")
    return sorted(set(messages))


def _run(function, cases, checks, verbose):
    failures = {label: [] for label in checks}
    syntax = source_violations(function)
    if syntax:
        for label in failures:
            failures[label].extend(syntax)
    for name, inputs, expected in cases:
        try:
            result = _call(function, inputs, len(expected))
        except Exception as exc:
            for label in failures:
                failures[label].append(f"{name}: {type(exc).__name__}: {exc}")
            continue
        for label, columns in checks.items():
            try:
                for index, kind in columns:
                    _array(result[index], expected[index], kind)
            except Exception as exc:
                failures[label].append(f"{name}: {exc}")
    if verbose:
        for label, messages in failures.items():
            print(f"{'FAIL' if messages else 'PASS'} - {label}")
            for message in messages:
                print(f"  {message}")
        if syntax is None:
            print("Source unavailable here: no-loop compliance needs manual review.")
    return not any(failures.values())


def check_part_a(function, verbose=True):
    cases = [
        ("paper example", ([2, 5, 8], [0, 4, 6, 11], [0, 1, 0], [0, 0, 1, 0]),
         ([[4, 4, 16, 81], [25, 1, 1, 36], [64, 16, 4, 9]],
          [[True, True, False, True], [False, False, True, False], [True, True, False, True]])),
        ("equal lengths still require all pairs", ([-1, 2], [1, 4], [42, 10], [10, 42]),
         ([[4, 25], [1, 4]], [[False, True], [True, False]])),
        ("one measurement and fractions", ([0.5], [-0.5, 0.5, 1.5], [7], [7, 9, 7]),
         ([[1, 0, 1]], [[True, False, True]])),
        ("one reusable reference", ([1, 3, 1], [1], [8, 8, 8], [8]),
         ([[0], [4], [0]], [[True], [True], [True]])),
    ]
    return _run(function, cases, {
        "A1: all-pairs costs": [(0, "float")],
        "A2: Boolean eligibility": [(1, "bool")],
    }, verbose)


def check_part_b(function, verbose=True):
    cases = [
        ("independent example", ([[9, 1, 4], [0, 16, 25]],
                                  [[True, False, True], [False, True, True]]),
         ([2, 1], [4, 16], [4, 10])),
        ("paper matrices", ([[4, 4, 16, 81], [25, 1, 1, 36], [64, 16, 4, 9]],
                            [[True, True, False, True], [False, False, True, False], [True, True, False, True]]),
         ([0, 2, 3], [4, 1, 9], [4, 2.5, 14 / 3])),
        ("ties and genuine zero costs", ([[0, 0, 5], [4, 1, 1]],
                                        [[False, True, True], [False, True, True]]),
         ([1, 1], [0, 1], [0, 0.5])),
        ("single row", ([[9, 4, 4]], [[True, True, True]]), ([1], [4], [4])),
        ("single column and reuse", ([[4], [0], [9]], [[True], [True], [True]]),
         ([0, 0, 0], [4, 0, 9], [4, 2, 13 / 3])),
        ("single cell", ([[0]], [[True]]), ([0], [0], [0])),
    ]
    return _run(function, cases, {
        "B1: eligible choices and paired costs": [(0, "integer"), (1, "float")],
        "B2: full running-average vector": [(2, "float")],
    }, verbose)


def load_file(path):
    path = Path(path).resolve()
    spec = importlib.util.spec_from_file_location("submitted_functions", path)
    if spec is None or spec.loader is None:
        raise ValueError(f"Cannot load Python file: {path}")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("file", type=Path)
    args = parser.parse_args()
    module = load_file(args.file)
    passed_a = check_part_a(module.build_comparisons)
    passed_b = check_part_b(module.choose_and_summarize)
    print("Local feedback only; no files were submitted or uploaded.")
    return 0 if passed_a and passed_b else 1


if __name__ == "__main__":
    raise SystemExit(main())
