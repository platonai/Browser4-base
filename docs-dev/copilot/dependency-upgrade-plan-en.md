# Browser4 Dependency Upgrade Plan

## Document Information
- **Created**: 2026-01-14
- **Project Version**: 4.5.0
- **Objective**: Establish systematic dependency upgrade strategy and process

---

## 1. Project Overview

### 1.1 Project Structure
Browser4 is a multi-module Maven project with **41 POM files** across **11 core modules**:

- `pulsar-parent`: Parent POM managing plugin versions
- `pulsar-dependencies`: Centralized external dependency management
- `pulsar-core`: Core engine module
- `pulsar-rest`: REST API module
- `pulsar-client`: Client SDK
- `pulsar-tests-common`: Common test utilities
- `pulsar-tests`: Integration tests
- `pulsar-benchmarks`: Performance benchmarks
- `pulsar-bom`: Dependency BOM
- `browser4-agents`: Browser agents
- `browser4-spa`: SPA application

### 1.2 Current Technology Stack

| Category | Library | Current Version |
|----------|---------|-----------------|
| Language | Java | 17 |
| Language | Kotlin | 2.2.21 |
| Language | Kotlin Coroutines | 1.10.2 |
| Framework | Spring Boot | 4.0.1 |
| Framework | Ktor | 3.3.1 |
| AI | LangChain4j | 1.5.0 |
| Utils | Guava | 33.2.1-jre |
| Utils | Apache Commons IO | 2.16.1 |
| Web | Apache HttpClient | 4.5.13 |
| Parser | Apache Tika | 2.9.0 |
| Document | Apache POI | 5.3.0 |
| NLP | Apache Lucene | 7.3.1 |
| Graph | JGraphT | 1.0.1 |
| Testing | JUnit 5 | 5.12.1 |
| Testing | MockK | 1.14.6 |

---

## 2. Upgrade Strategy

### 2.1 Priority Levels

**P0 - Critical (Must Upgrade)**
- Security vulnerabilities with published CVEs
- Compatibility issues with JDK/Kotlin
- Blocking bugs in core functionality

**P1 - High (Should Upgrade)**
- Significant performance improvements (>10%)
- Critical new features
- End of lifecycle approaching

**P2 - Medium (Planned Upgrade)**
- Minor version updates with bug fixes
- Dependency conflict resolution
- Ecosystem alignment

**P3 - Low (Optional Upgrade)**
- Documentation improvements only
- Internal refactoring without API changes
- Experimental features

### 2.2 Upgrade Order

```
Phase 1: Infrastructure
  └─> JDK → Maven → Maven Plugins

Phase 2: Core Frameworks
  └─> Kotlin → Kotlin Coroutines → Spring Boot

Phase 3: Core Dependencies
  └─> Bottom-up by dependency depth

Phase 4: Testing Frameworks
  └─> JUnit → MockK → Mockito

Phase 5: Other Dependencies
  └─> By priority in batches
```

### 2.3 Risk Matrix

| Risk Type | Likelihood | Impact | Mitigation |
|-----------|------------|--------|------------|
| API Incompatibility | Medium | High | Progressive upgrade, thorough testing |
| Performance Degradation | Low | High | Benchmark comparison |
| Dependency Conflicts | Medium | Medium | Exclusions, BOM management |
| Security Vulnerabilities | Low | High | Regular scanning, quick response |
| Build Failures | Low | Medium | CI/CD auto-validation |

---

## 3. Specific Upgrade Plans

### 3.1 Security Fixes (P0)

**Known Issues**:
- Apache Lucene 7.3.1: Outdated, recommend upgrade to 9.x
- Apache HttpClient 4.5.13: Consider migration to 5.x
- Hadoop 2.7.2: Very old, consider removal or isolation

**Action Items**:
1. Enable OWASP Dependency Check
2. Generate vulnerability reports
3. Fix critical vulnerabilities by priority

### 3.2 Core Framework Upgrades (P1)

**Spring Boot**
- Current: 4.0.1
- Target: Stay current with 4.0.x patches
- Note: 4.0 is new, requires thorough testing

**Kotlin**
- Current: 2.2.21
- Target: Keep up with latest stable 2.2.x
- Note: Monitor compiler and coroutine improvements

**Ktor**
- Current: 3.3.1
- Target: Keep up with latest 3.x
- Dependency: Maintain compatibility with Kotlin version

### 3.3 Special Cases

#### Apache Lucene (7.3.1 → 9.x)
- Major API changes
- Index format incompatibility
- Strategy: Create compatibility layer, progressive migration

#### Apache HttpClient (4.5.x → 5.x)
- Package rename (org.apache.http → org.apache.hc)
- API redesign
- Strategy: Evaluate Ktor as alternative, or gradual migration

#### Hadoop Removal
- Current use: Configuration management only
- Issue: Outdated, bloated, security risk
- Solution: Replace with Apache Commons Configuration

---

## 4. Tools & Automation

### 4.1 Maven Versions Plugin

```bash
# Check for available dependency updates
./mvnw versions:display-dependency-updates

# Check for available plugin updates
./mvnw versions:display-plugin-updates

# Check for property updates
./mvnw versions:display-property-updates

# Update to specific version
./mvnw versions:use-dep-version \
  -Dincludes=org.example:dependency-name \
  -DdepVersion=1.2.3
```

**Recommended Configuration** (add to `pulsar-parent/pom.xml`):

```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>versions-maven-plugin</artifactId>
    <version>2.18.0</version>
    <configuration>
        <generateBackupPoms>false</generateBackupPoms>
        <rulesUri>file://${project.basedir}/maven-version-rules.xml</rulesUri>
    </configuration>
</plugin>
```

### 4.2 OWASP Dependency Check

**Current Status**: Configured but disabled (`<skip>true</skip>`)

**Enable by**: Set `<skip>false</skip>` and configure:

```xml
<plugin>
    <groupId>org.owasp</groupId>
    <artifactId>dependency-check-maven</artifactId>
    <version>11.2.0</version>
    <configuration>
        <skip>false</skip>
        <failBuildOnCVSS>7</failBuildOnCVSS>
        <format>ALL</format>
    </configuration>
</plugin>
```

**Usage**:
```bash
# Run security check
./mvnw dependency-check:check

# Generate aggregate report
./mvnw dependency-check:aggregate
```

### 4.3 Dependency Analysis

```bash
# View full dependency tree
./mvnw dependency:tree

# View specific module's dependencies
./mvnw -pl pulsar-core dependency:tree

# Analyze dependency usage
./mvnw dependency:analyze

# Find unused dependencies
./mvnw dependency:analyze -DignoreNonCompile=true
```

### 4.4 Automation Script

Create `bin/tools/check-dependencies.sh`:

```bash
#!/bin/bash
# Dependency check script

echo "=== Checking for dependency updates ==="
./mvnw versions:display-dependency-updates -q

echo ""
echo "=== Checking for plugin updates ==="
./mvnw versions:display-plugin-updates -q

echo ""
echo "=== Checking for security vulnerabilities ==="
./mvnw dependency-check:check -q

echo ""
echo "=== Analyzing dependencies ==="
./mvnw dependency:analyze -q
```

---

## 5. Upgrade Process

### 5.1 Standard Workflow

```
1. Research Phase
   ├─ Review CHANGELOG
   ├─ Assess impact scope
   ├─ Identify breaking changes
   └─ Determine upgrade path

2. Preparation Phase
   ├─ Create upgrade branch
   ├─ Backup current config
   └─ Prepare rollback plan

3. Implementation Phase
   ├─ Update version numbers
   ├─ Resolve compile errors
   ├─ Adapt API changes
   └─ Update configuration

4. Testing Phase
   ├─ Unit tests
   ├─ Integration tests
   ├─ Performance tests
   └─ Regression tests

5. Validation Phase
   ├─ Code review
   ├─ CI/CD validation
   ├─ Security scan
   └─ Documentation update

6. Release Phase
   ├─ Merge to main
   ├─ Update version history
   ├─ Release notes
   └─ Monitor
```

### 5.2 Git Workflow

```bash
# 1. Create upgrade branch
git checkout -b upgrade/dependency-name-version

# 2. Perform upgrade
# ... modify pom.xml files ...

# 3. Test & verify
./mvnw clean test

# 4. Commit changes
git add .
git commit -m "deps: upgrade dependency-name from x.y.z to a.b.c"

# 5. Push and create PR
git push origin upgrade/dependency-name-version
```

### 5.3 Commit Message Format

Follow Conventional Commits:

```
deps: upgrade <package> from <old> to <new>

- Key change 1
- Key change 2
- Test coverage notes

Breaking Changes: (if any)
- Incompatible change description

Refs: #issue-number
```

---

## 6. Testing Strategy

### 6.1 Test Levels

**Level 1: Compilation**
```bash
./mvnw clean compile -DskipTests
```
Goal: Ensure code compiles successfully

**Level 2: Unit Tests**
```bash
./mvnw test
```
Goal: Verify core functionality

**Level 3: Integration Tests**
```bash
./mvnw verify
```
Goal: Verify module interactions

**Level 4: Performance Tests**
```bash
./mvnw -pl pulsar-benchmarks exec:java
```
Goal: Ensure no significant performance regression (threshold: ±5%)

**Level 5: E2E Tests**
```bash
bin/tests/run-e2e-tests.sh
```
Goal: Verify complete user scenarios

### 6.2 Coverage Requirements

- **New code**: ≥ 80%
- **Modified code**: Maintain or improve existing coverage
- **Core modules**: ≥ 70% (current requirement)

---

## 7. CI/CD Integration

### 7.1 Dependency Security Check Workflow

Create `.github/workflows/dependency-check.yml`:

```yaml
name: Dependency Security Check

on:
  schedule:
    - cron: '0 1 * * 1'  # Every Monday
  workflow_dispatch:

jobs:
  dependency-check:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v4

    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
        cache: 'maven'

    - name: Run OWASP Dependency Check
      run: ./mvnw dependency-check:check

    - name: Upload Report
      if: always()
      uses: actions/upload-artifact@v4
      with:
        name: dependency-check-report
        path: target/dependency-check-report.html
```

### 7.2 Dependency Update Check

Create `.github/workflows/dependency-updates.yml`:

```yaml
name: Check Dependency Updates

on:
  schedule:
    - cron: '0 2 1 * *'  # Monthly
  workflow_dispatch:

jobs:
  check-updates:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v4

    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
        cache: 'maven'

    - name: Check for updates
      run: |
        ./mvnw versions:display-dependency-updates > updates.txt
        ./mvnw versions:display-plugin-updates >> updates.txt

    - name: Create Issue
      uses: actions/github-script@v7
      with:
        script: |
          const fs = require('fs');
          const updates = fs.readFileSync('updates.txt', 'utf8');

          await github.rest.issues.create({
            owner: context.repo.owner,
            repo: context.repo.repo,
            title: '📦 Monthly Dependency Update Report',
            body: '## Available Updates\n\n```\n' + updates + '\n```',
            labels: ['dependencies', 'maintenance']
          });
```

---

## 8. Best Practices

### 8.1 DO's ✅

1. **Small steps**: Upgrade few dependencies at a time
2. **Test thoroughly**: Run complete test suite for each upgrade
3. **Read documentation**: Carefully review CHANGELOG and migration guides
4. **Keep records**: Document upgrade reasons and test results
5. **Stay current**: Don't let dependencies fall too far behind
6. **Automate**: Use tools for automated checks
7. **Security first**: Prioritize security vulnerability fixes
8. **Version pinning**: Explicitly specify versions

### 8.2 DON'Ts ❌

1. **Avoid blind upgrades**: Never upgrade without testing
2. **Avoid batch upgrades**: Don't upgrade too many at once
3. **Avoid version jumps**: Try to avoid skipping major versions
4. **Avoid SNAPSHOT**: Don't use SNAPSHOT versions in production
5. **Avoid transitive dependencies**: Don't depend on internals of transitive deps
6. **Avoid conflicts**: Watch for version conflicts in dependency tree
7. **Avoid skipping tests**: Never skip any testing phase

### 8.3 Common Issues

**Q: Build fails after upgrade?**
- Check compile errors
- Review dependency conflicts (`mvn dependency:tree`)
- Consult official migration guide
- Rollback if cannot resolve quickly

**Q: How to handle dependency conflicts?**
```xml
<!-- Method 1: Exclude conflicting dependency -->
<dependency>
    <groupId>com.example</groupId>
    <artifactId>library</artifactId>
    <exclusions>
        <exclusion>
            <groupId>conflicting-group</groupId>
            <artifactId>conflicting-artifact</artifactId>
        </exclusion>
    </exclusions>
</dependency>

<!-- Method 2: Force version in dependencyManagement -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>conflicting-group</groupId>
            <artifactId>conflicting-artifact</artifactId>
            <version>desired-version</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

---

## 9. Implementation Timeline (Example)

### Q1: Foundation
- **Weeks 1-2**: Setup tools and generate reports
- **Weeks 3-4**: Security fixes (P0)
- **Weeks 5-8**: Core framework updates (P1)

### Q2: Libraries
- **Weeks 1-4**: Utility library upgrades (P2)
- **Weeks 5-8**: Testing framework upgrades (P2)

### Q3: Major Upgrades
- **Weeks 1-6**: Apache Lucene 7.x → 9.x (P1)
- **Weeks 7-8**: Technical debt cleanup

### Q4: Optimization
- **Weeks 1-4**: Optimization and refinement
- **Weeks 5-8**: Annual review and next year planning

---

## 10. References

### Official Documentation
- [Maven Versions Plugin](https://www.mojohaus.org/versions-maven-plugin/)
- [OWASP Dependency Check](https://owasp.org/www-project-dependency-check/)
- [Spring Boot Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide)
- [Kotlin Release Notes](https://kotlinlang.org/docs/releases.html)

### Tools
- [Maven Central Search](https://search.maven.org/)
- [mvnrepository.com](https://mvnrepository.com/)
- [Snyk Vulnerability Database](https://snyk.io/vuln/)
- [CVE Details](https://www.cvedetails.com/)

---

## Appendix: Quick Reference

### Key Commands

```bash
# Dependency Management
./mvnw versions:display-dependency-updates  # Check dependency updates
./mvnw versions:display-plugin-updates      # Check plugin updates
./mvnw versions:update-properties           # Update version properties

# Security
./mvnw dependency-check:check               # Run security scan
./mvnw dependency-check:aggregate           # Aggregate report

# Analysis
./mvnw dependency:tree                      # View dependency tree
./mvnw dependency:analyze                   # Analyze dependencies
./mvnw dependency:list                      # List all dependencies

# Testing
./mvnw clean test                           # Unit tests
./mvnw clean verify                         # Include integration tests
./mvnw -pl pulsar-core test                 # Test single module
```

### Checklist

**Before Upgrade:**
- [ ] Read CHANGELOG
- [ ] Review Migration Guide
- [ ] Check known issues
- [ ] Assess impact
- [ ] Prepare rollback plan

**After Upgrade:**
- [ ] Compilation successful
- [ ] Unit tests pass
- [ ] Integration tests pass
- [ ] No performance regression
- [ ] Security scan passes
- [ ] Documentation updated
- [ ] CI/CD passes

---

**Document Maintenance**:
This document should be continuously updated as the project evolves.

**Feedback**:
For issues or suggestions, please create an issue or contact the maintenance team.

---
*Last updated: 2026-01-14*
