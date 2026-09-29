# AI Agent Development Instructions (Spec-Driven Workflow)

## 1. Operating Principles
You are an AI development agent executing a spec-driven port of a legacy Windows application to macOS. You must operate entirely through a test-driven pipeline, strictly adhering to the GitHub-centric issue and pull request workflow defined below. 

## 2. Environment & Tooling Standards
All development must be standardized and reproducible:
* **Makefiles:** Serve as the unified entry point for all build, test, and run commands.
* **DevContainers:** Ensure local development isolation and reproducibility.
* **Linters & CI:** Code must pass all strict linting and automated tests before a PR is opened.
* **Platform:** The primary target is macOS. 

## 3. Scope & Evaluation Workflow (Strictly Enforced)
You are not permitted to write application code until the scope and evaluation metrics are documented in GitHub Issues.

1. **Issue Creation (`gh` CLI):** 
   * For every feature, refactor, or porting step, use the `gh issue create` command to generate a long-horizon, detailed checklist.
   * Each issue must contain a strict "Evaluation Criteria" section defining exactly how the code will be tested and verified on a Mac environment.
2. **Execution Block:** 
   * Do not write or modify application logic until the evaluation details in the corresponding GitHub issue are locked and achievable.
   * Drive all changes through a test-driven pipeline, ensuring the tests match the issue's evaluation checklist.
3. **Pull Requests (`gh` CLI):** 
   * Once evaluation criteria are achieved, bundle the changes into a Pull Request using `gh pr create`.
   * The PR description must be detailed, directly linking to the issue, and explicitly explaining how the code changes satisfy the original checklist.

## 4. Platform & Build Constraints
* **Target OS:** macOS (Primary) / Cross-platform.
* **Build Artifacts:** Must compile into a self-sufficient, easily runnable artifact (e.g., runnable JAR, shell-wrapped binary, or GraalVM executable).
* **Release Pipeline:** The repository must use GitHub Actions to automate the build and push artifacts to a public GitHub Release.
* **Code Signing Bypass:** As this project operates without an Apple Developer Program subscription, the build process must not rely on official macOS notarization. 
* **Distribution:** Release notes and documentation must include clear instructions for the end-user on how to bypass Gatekeeper warnings (e.g., via standard `xattr -d com.apple.quarantine <file>` commands or `Ctrl+Click -> Open` execution) for ad-hoc signed or unsigned binaries.