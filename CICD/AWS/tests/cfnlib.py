"""Load a CloudFormation template without calling AWS."""

from pathlib import Path

import yaml


class CfnLoader(yaml.SafeLoader):
    pass


def _construct(loader, tag_suffix, node):
    if isinstance(node, yaml.ScalarNode):
        value = loader.construct_scalar(node)
    elif isinstance(node, yaml.SequenceNode):
        value = loader.construct_sequence(node)
    else:
        value = loader.construct_mapping(node)
    return {tag_suffix: value}


CfnLoader.add_multi_constructor("!", _construct)


def load_template(path: Path) -> dict:
    return yaml.load(path.read_text(), Loader=CfnLoader)


def template_dir() -> Path:
    return Path(__file__).resolve().parents[1] / "CloudFormation"
