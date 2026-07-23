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
- Do not mention added or updated tests; the mandatory 100% code coverage rule makes test changes implicit.

## Specialized Skills
The following specialized playbooks are available in `.kiro/skills/`:
- **copyright-manager**: Follow this for all file headers.
- **java-quality**: Follow this for java and test development.
- **test-quality**: Follow this for test development.
 
