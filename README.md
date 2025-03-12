# Kotlin Conventional Commit Generator

A simple command-line tool written in Kotlin that helps generate conventional commit messages based on a YAML-inspired configuration file, without external dependencies.

## Features

- Follows the [Conventional Commits](https://www.conventionalcommits.org/) specification
- Customizable commit types, scopes, and other options through a YAML-inspired configuration file
- Interactive CLI prompts to guide you through creating well-formatted commits
- Support for breaking changes and issue references
- No external libraries required

## Requirements

- Kotlin 1.5+
- JDK 8+
- Git

## Installation

### Using Scoop (Windows)

1. Add the Malefic bucket to Scoop:

    ```sh
    scoop bucket add malefic https://github.com/OmyDaGreat/MaleficBucket
    ```

2. Install Kommit:
    
    ```sh
    scoop install kommit
    ```

## Usage

1. Place the `.kommit.yaml` file in your project root or specify a custom path.
2. Run the script:

    ```sh
    kommit
    ```

3. Follow the prompts to generate your commit message.

## Configuration File Format

The configuration file uses a YAML-inspired format:

```yaml
# Comments start with #

# Commit Types
types:
  - feat: A new feature
  - fix: A bug fix
  - docs: Documentation only changes

# Available Scopes
scopes:
  - core
  - ui
  - api
  - docs

# Configuration Options
options:
  allowCustomScopes: true
  allowEmptyScopes: false
  issuePrefix: "ISSUES CLOSED:"
  changesPrefix: "BREAKING CHANGE:"
  allowBreakingChanges:
    - feat
    - fix
    - refactor
  allowIssues:
    - feat
    - fix
    - docs
```

### Configuration Options

- `types`: List of commit types with their descriptions
- `scopes`: List of available scopes
- `options.allowCustomScopes`: Whether to allow custom scopes (true/false)
- `options.allowEmptyScopes`: Whether to allow empty scopes (true/false)
- `options.allowBreakingChanges`: List of types that can have breaking changes
- `options.allowIssues`: List of types that can have issue references
- `options.issuePrefix`: The prefix used for issue references in the footer
- `options.changesPrefix`: The prefix used for breaking changes in the footer

## Example Commit Message

```
feat(ui)!: add new button component

This adds a new reusable button component with various styles and sizes.

BREAKING CHANGE: Changes the API for button styling

ISSUES CLOSED: #123, #456
```
