# Global Project Rules
- Language: Java 21
- Build: Maven
- Framework: Spring Boot 4.0+

## Code Style
- **Simple is good, complex is bad** — always prefer readable code over clever code
- Use traditional for loops instead of streams unless streams provide a clear advantage (e.g., parallel processing, chaining multiple transformations concisely)
- Prefer straightforward null checks over Optional chains
- Avoid over-engineering: solve the problem at hand, don't abstract prematurely

## Commit Messages
- Follow [ONAP commit message guidelines](https://lf-onap.atlassian.net/wiki/spaces/DW/pages/16390326/Commit+Message+Guidelines)
- Format: `<type>: <short description>` (max 72 chars subject line)
- Types: feat, fix, refactor, test, docs, chore
- **Always include Issue-ID (Jira ticket) in the footer. If unknown, ask the user before generating the commit message.**
- Body should explain *why*, not *what* (the code shows what)
- Prefer a short list of bullets in the body over prose paragraphs
- Do not include Signed-off-by lines; the user will handle sign-off via git command line
- Do not mention added or updated tests; the mandatory 100% code coverage rule makes test changes implicit.

## Build & Test Execution
- Do NOT run `mvn` (or any build/compile/test command) yourself to compile or run tests.
- Instead, hand over to the human: tell them which tests/specs to run (and the module) so they can build and run them in their IDE.

## Multi-commit Workflow
- When a task is planned as two or more commits, implement ONLY the first commit's scope, then STOP and hand back for review, commit, and push before starting the next commit.
- Do not make all the changes for every planned commit in one pass.
- Wait for the user to confirm before proceeding to each subsequent commit.

## Test Ordering
- Cover the happy/normal path first, then the negative/error paths.
- This applies to the order of test methods within a spec AND to the row order of data-driven `where:` tables (put the accepted/success case before the rejected/failure case).

## Specialized Skills
The following specialized playbooks are available in `.kiro/skills/`:
- **copyright-manager**: Follow this for all file headers.
- **java-quality**: Follow this for java and test development.
- **test-quality**: Follow this for test development.
 
