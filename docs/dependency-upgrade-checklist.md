# Dependency Upgrade Checklist Template

**Dependency:** [Name]  
**Current Version:** [X.Y.Z]  
**Target Version:** [A.B.C]  
**Priority:** [ ] P0-Critical [ ] P1-High [ ] P2-Medium [ ] P3-Low  
**Date:** YYYY-MM-DD  
**Owner:** @username

---

## Pre-Upgrade Checklist

### Research Phase
- [ ] Review official CHANGELOG/Release Notes
- [ ] Check migration guide (if available)
- [ ] Search for known issues in GitHub/Stack Overflow
- [ ] Identify breaking changes
- [ ] Check compatibility with current Java/Kotlin version
- [ ] Review dependencies of the dependency (transitive deps)

### Impact Assessment
- [ ] List all modules using this dependency
- [ ] Identify potentially affected code
- [ ] Estimate effort (hours/days)
- [ ] Determine if this is a major/minor/patch upgrade
- [ ] Check if other dependencies need concurrent upgrade

### Planning
- [ ] Create upgrade branch: `upgrade/[dependency-name]-[version]`
- [ ] Schedule testing time
- [ ] Prepare rollback plan
- [ ] Notify team if this is a major change

---

## Upgrade Checklist

### Implementation
- [ ] Update version in `pulsar-dependencies/pom.xml` or appropriate POM
- [ ] Update `maven-version-rules.xml` if needed
- [ ] Check for dependency exclusions that need updating
- [ ] Search codebase for deprecated API usage
- [ ] Update imports if package names changed
- [ ] Fix compilation errors
- [ ] Update configuration files if needed
- [ ] Update documentation references

### Code Changes
- [ ] Adapt code to API changes
- [ ] Remove workarounds for bugs fixed in new version
- [ ] Update error handling if error types changed
- [ ] Review and update comments referencing old behavior

---

## Testing Checklist

### Build & Compilation
- [ ] Clean build: `./mvnw clean compile -DskipTests`
- [ ] No compilation warnings introduced
- [ ] Check dependency tree: `./mvnw dependency:tree`
- [ ] Verify no unwanted transitive dependency changes

### Unit Tests
- [ ] Run all unit tests: `./mvnw test`
- [ ] All existing tests pass
- [ ] Add new tests for new functionality (if applicable)
- [ ] Verify test coverage maintained/improved

### Integration Tests
- [ ] Run integration tests: `./mvnw verify`
- [ ] Test affected modules specifically
- [ ] Verify module interactions
- [ ] Check for resource leaks

### Module-Specific Tests
- [ ] Test `pulsar-core`: `./mvnw -pl pulsar-core test`
- [ ] Test `pulsar-rest`: `./mvnw -pl pulsar-rest test`
- [ ] Test `pulsar-browser` (if affected)
- [ ] Test `pulsar-client` (if affected)

### Performance Testing
- [ ] Run benchmarks: `./mvnw -pl pulsar-benchmarks verify`
- [ ] Compare performance metrics (baseline vs upgraded)
- [ ] Performance change within acceptable range (±5%)
- [ ] No memory leaks detected
- [ ] CPU usage comparable

### Security Testing
- [ ] Run OWASP check: `./mvnw dependency-check:check`
- [ ] No new high/critical vulnerabilities introduced
- [ ] Existing vulnerabilities resolved (if that was the goal)

### Manual Testing
- [ ] Test common user scenarios
- [ ] Verify browser automation works
- [ ] Test REST API endpoints (if affected)
- [ ] Check logging output
- [ ] Verify error messages are clear

---

## Validation Checklist

### Code Quality
- [ ] Code review completed
- [ ] No code smells introduced
- [ ] Follows project coding standards
- [ ] Comments and documentation updated
- [ ] No debug code or print statements left

### CI/CD
- [ ] All CI checks pass
- [ ] Docker build succeeds (if applicable)
- [ ] No flaky tests introduced
- [ ] Build time acceptable

### Documentation
- [ ] Update `docs/dependency-upgrade-plan.md` version table
- [ ] Update CHANGELOG.md
- [ ] Update migration notes (if breaking changes)
- [ ] Update README.md (if user-facing changes)

### Compatibility
- [ ] Works with Java 17
- [ ] Compatible with current Kotlin version
- [ ] Works on Linux
- [ ] Works on macOS
- [ ] Works on Windows

---

## Pre-Merge Checklist

### Review
- [ ] Self-review of all changes
- [ ] Peer review completed
- [ ] All review comments addressed
- [ ] No merge conflicts

### Final Validation
- [ ] Rebase on latest main/master
- [ ] Final test run passes
- [ ] No last-minute changes needed
- [ ] Commit messages follow conventions

### Communication
- [ ] PR description complete
- [ ] Breaking changes highlighted
- [ ] Migration instructions provided (if needed)
- [ ] Team notified of significant changes

---

## Post-Merge Checklist

### Monitoring
- [ ] Monitor CI/CD for the next few days
- [ ] Watch for user-reported issues
- [ ] Check error tracking systems
- [ ] Monitor performance metrics

### Documentation
- [ ] Update dependency version tracking document
- [ ] Archive this checklist in upgrade history
- [ ] Share learnings with team
- [ ] Update upgrade plan if needed

### Cleanup
- [ ] Delete upgrade branch (after merge)
- [ ] Close related issues
- [ ] Update project board/tracking

---

## Rollback Plan

**If problems are discovered after merge:**

1. Identify the issue:
   - [ ] Document the problem
   - [ ] Determine severity
   - [ ] Check if quick fix is possible

2. Decision point:
   - [ ] Can fix forward quickly (<2 hours)? → Fix and deploy
   - [ ] Cannot fix quickly? → Rollback

3. Rollback procedure:
   ```bash
   # Create rollback branch
   git checkout -b rollback/[dependency-name]-[version]
   
   # Revert the merge commit
   git revert -m 1 [merge-commit-sha]
   
   # Or manually restore old version
   # Edit pom.xml to restore previous version
   
   # Test
   ./mvnw clean test
   
   # Push and create emergency PR
   git push origin rollback/[dependency-name]-[version]
   ```

4. Post-rollback:
   - [ ] Notify team
   - [ ] Document root cause
   - [ ] Create issue to track resolution
   - [ ] Plan for retry with fixes

---

## Notes

### Breaking Changes Found
- [ ] Breaking change 1: [description]
- [ ] Breaking change 2: [description]

### Issues Encountered
- [ ] Issue 1: [description and resolution]
- [ ] Issue 2: [description and resolution]

### Performance Impact
- Baseline: [metric]
- After upgrade: [metric]
- Change: [+/-X%]

### Time Tracking
- Research: [X hours]
- Implementation: [X hours]
- Testing: [X hours]
- Review: [X hours]
- **Total: [X hours]**

### References
- Release notes: [URL]
- Migration guide: [URL]
- Related issues: #[issue-number]
- Related PRs: #[pr-number]

---

## Sign-off

- [ ] **Developer**: I have completed all items and this is ready for review
  - Signature: _________________ Date: _______

- [ ] **Reviewer**: I have reviewed the changes and approve this upgrade
  - Signature: _________________ Date: _______

- [ ] **Maintainer**: I approve this for merging
  - Signature: _________________ Date: _______
