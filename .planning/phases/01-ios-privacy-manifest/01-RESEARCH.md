# Phase 1: iOS Privacy Manifest - Research

**Researched:** 2026-01-15
**Domain:** Apple iOS Privacy Manifest for Kotlin Multiplatform / Compose Multiplatform
**Confidence:** HIGH

<research_summary>
## Summary

Researched Apple's iOS Privacy Manifest requirements for App Store submission, specifically for Kotlin Multiplatform and Compose Multiplatform applications. Since Spring 2024, Apple requires all iOS apps to include a `PrivacyInfo.xcprivacy` file declaring usage of "Required Reason APIs" - system APIs that could potentially be used for fingerprinting.

The key finding is that Compose Multiplatform apps use `fstat`, `stat`, and potentially `mach_absolute_time` internally (from file operations and timing functions), even though they're not used for tracking. These must be declared with appropriate reason codes.

The project uses Kotlin 2.3.0, which is above the 1.9.20 threshold where `mach_absolute_time` was removed from the default allocator. However, Compose Multiplatform's file operations still trigger the File Timestamp API requirements.

**Primary recommendation:** Create a `PrivacyInfo.xcprivacy` file in `iosApp/iosApp/` with declarations for NSPrivacyAccessedAPICategoryFileTimestamp (reason code 0A2A.1) to cover Compose Multiplatform's internal file operations. Add to Xcode target's bundle resources.
</research_summary>

<standard_stack>
## Standard Stack

### Core
| Library/Tool | Version | Purpose | Why Standard |
|--------------|---------|---------|--------------|
| PrivacyInfo.xcprivacy | N/A | Privacy manifest file | Apple requirement since May 2024 |
| Xcode 15+ | 15.0+ | Create/edit privacy manifests | Built-in App Privacy File template |

### Supporting
| Tool | Purpose | When to Use |
|------|---------|-------------|
| JetBrains Required Reason Finder | Python script to detect API usage | Before submission to verify all APIs declared |
| Xcode Privacy Report | Generate privacy report from archive | Pre-submission validation |
| apple-privacy-manifests Gradle plugin | Bundle manifest with Kotlin artifacts | When distributing as framework (optional) |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Manual manifest | JetBrains privacy plugin | Plugin adds complexity; manual is simpler for single app |
| Xcode visual editor | Raw XML editing | Xcode provides dropdowns for valid values, reduces errors |

**File Location:**
```
mobile/iosApp/iosApp/PrivacyInfo.xcprivacy
```

This file goes in the iOS app bundle alongside `Info.plist`.
</standard_stack>

<architecture_patterns>
## Architecture Patterns

### Privacy Manifest File Structure
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>NSPrivacyTracking</key>
    <false/>
    <key>NSPrivacyTrackingDomains</key>
    <array/>
    <key>NSPrivacyCollectedDataTypes</key>
    <array/>
    <key>NSPrivacyAccessedAPITypes</key>
    <array>
        <!-- Required reason API declarations go here -->
    </array>
</dict>
</plist>
```

### Pattern 1: Declaring Required Reason APIs
**What:** Each API category needs a dictionary with type and reasons
**When to use:** For every required reason API your app uses
**Example:**
```xml
<dict>
    <key>NSPrivacyAccessedAPIType</key>
    <string>NSPrivacyAccessedAPICategoryFileTimestamp</string>
    <key>NSPrivacyAccessedAPITypeReasons</key>
    <array>
        <string>0A2A.1</string>
    </array>
</dict>
```

### Pattern 2: Multiple API Categories
**What:** Declare multiple API categories in the array
**When to use:** When app uses multiple required reason APIs
**Example:**
```xml
<key>NSPrivacyAccessedAPITypes</key>
<array>
    <dict>
        <key>NSPrivacyAccessedAPIType</key>
        <string>NSPrivacyAccessedAPICategoryFileTimestamp</string>
        <key>NSPrivacyAccessedAPITypeReasons</key>
        <array>
            <string>0A2A.1</string>
        </array>
    </dict>
    <dict>
        <key>NSPrivacyAccessedAPIType</key>
        <string>NSPrivacyAccessedAPICategoryUserDefaults</string>
        <key>NSPrivacyAccessedAPITypeReasons</key>
        <array>
            <string>CA92.1</string>
        </array>
    </dict>
</array>
```

### Anti-Patterns to Avoid
- **Wrong reason code:** Using C617.1 when 0A2A.1 is correct causes ITMS-91056 errors
- **Missing Target Membership:** File not included in build causes warnings despite existing
- **Wrong file name:** Must be exactly `PrivacyInfo.xcprivacy` (case-sensitive)
</architecture_patterns>

<dont_hand_roll>
## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| API detection | Manual code search | JetBrains Required Reason Finder script | Catches APIs in dependencies you don't know about |
| Reason code selection | Guessing from descriptions | Apple's official documentation | Wrong codes cause submission rejection |
| File format validation | Manual XML editing | Xcode visual editor | Dropdowns ensure valid values |
| Submission validation | Manual testing | Xcode "Generate Privacy Report" | Catches missing declarations before submission |

**Key insight:** Apple's requirements are specific and enforcement is automated. Using their official tools and documentation prevents rejection. The JetBrains script is particularly important because Kotlin/Native and Compose Multiplatform use APIs internally that aren't visible in your application code.
</dont_hand_roll>

<common_pitfalls>
## Common Pitfalls

### Pitfall 1: File Not Included in Target
**What goes wrong:** App Store Connect still complains about missing privacy manifest
**Why it happens:** PrivacyInfo.xcprivacy created but not added to build target
**How to avoid:** In Xcode, select file → File Inspector → Target Membership → Check your app target
**Warning signs:** Warnings in email despite file existing in project

### Pitfall 2: Wrong Reason Code for SDK Usage
**What goes wrong:** ITMS-91056 error - incorrect reason code
**Why it happens:** Using app-specific reason (C617.1, 3B52.1) when SDK reason (0A2A.1) is required
**How to avoid:** For third-party SDKs and frameworks (including Compose MP), use 0A2A.1 for file timestamp
**Warning signs:** Rejection email mentions specific reason code

### Pitfall 3: Missing API Declaration
**What goes wrong:** ITMS-91053 error - Missing API declaration
**Why it happens:** App or dependency uses required API not declared in manifest
**How to avoid:** Run JetBrains Required Reason Finder before submission
**Warning signs:** Rejection email lists specific API (stat, fstat, mach_absolute_time)

### Pitfall 4: Kotlin Version Too Old
**What goes wrong:** mach_absolute_time warnings
**Why it happens:** Kotlin < 1.9.20 uses mimalloc allocator which calls mach_absolute_time
**How to avoid:** Use Kotlin 1.9.20+ (project uses 2.3.0 - OK)
**Warning signs:** SystemBootTime API warnings in submission

### Pitfall 5: Static vs Dynamic Linking Confusion
**What goes wrong:** Apple doesn't see SDK's privacy manifest
**Why it happens:** Static libraries don't bundle their own manifest automatically
**How to avoid:** For Compose MP (typically dynamic framework), manifest auto-processed; for static, declare in app
**Warning signs:** SDK-related APIs not covered by SDK's own manifest
</common_pitfalls>

<code_examples>
## Code Examples

### Complete Privacy Manifest for Compose Multiplatform App
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<!--
  PrivacyInfo.xcprivacy

  Privacy manifest for KMP Solo Developer Template
  Required for App Store submission since May 2024

  APIs declared:
  - File Timestamp (0A2A.1): Used by Compose Multiplatform internally
  - UserDefaults (CA92.1): Used by app for preferences storage
-->
<plist version="1.0">
<dict>
    <key>NSPrivacyTracking</key>
    <false/>
    <key>NSPrivacyTrackingDomains</key>
    <array/>
    <key>NSPrivacyCollectedDataTypes</key>
    <array/>
    <key>NSPrivacyAccessedAPITypes</key>
    <array>
        <!-- File Timestamp API - Compose Multiplatform uses stat/fstat internally -->
        <dict>
            <key>NSPrivacyAccessedAPIType</key>
            <string>NSPrivacyAccessedAPICategoryFileTimestamp</string>
            <key>NSPrivacyAccessedAPITypeReasons</key>
            <array>
                <string>0A2A.1</string>
            </array>
        </dict>
        <!-- UserDefaults API - App settings storage -->
        <dict>
            <key>NSPrivacyAccessedAPIType</key>
            <string>NSPrivacyAccessedAPICategoryUserDefaults</string>
            <key>NSPrivacyAccessedAPITypeReasons</key>
            <array>
                <string>CA92.1</string>
            </array>
        </dict>
    </array>
</dict>
</plist>
```

### JetBrains Required Reason Finder Usage
```bash
# Run from mobile/ directory where Kotlin framework is configured
cd /path/to/project/mobile
/usr/bin/python3 -c "$(curl -fsSL https://github.com/JetBrains/kotlin/raw/rrf_v0.0.1/libraries/tools/required-reason-finder/required_reason_finder.py)"
```

### Xcode Steps to Create Privacy Manifest
```
1. Open iosApp/iosApp.xcodeproj in Xcode
2. Select File > New > File...
3. Scroll to "Resource" section
4. Select "App Privacy File Type"
5. Click Next
6. Ensure "iosApp" target is checked
7. Click Create
8. File named "PrivacyInfo.xcprivacy" is created
9. Use visual editor to add required API declarations
```

### Xcode Steps to Generate Privacy Report
```
1. In Xcode, select Product > Archive
2. Wait for archive to complete
3. In Organizer, Control-click the archive
4. Select "Generate Privacy Report"
5. Review report for any missing declarations
```
</code_examples>

<sota_updates>
## State of the Art (2024-2026)

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| No privacy manifest | PrivacyInfo.xcprivacy required | May 1, 2024 | Mandatory for all submissions |
| Kotlin mimalloc allocator | Custom/std allocator default | Kotlin 1.9.20 | Removes mach_absolute_time usage |
| Manual API detection | JetBrains finder script | 2024 | Automated detection of required APIs |

**New requirements:**
- **Feb 12, 2025**: Third-party SDKs in commonly-used SDK list must include privacy manifest
- This affects apps using SDKs like Firebase, which now ship their own manifests

**Current best practice:**
- Compose Multiplatform 1.9.3+ ships with known API usage documented
- Kotlin 2.0+ removes most problematic API usage from runtime
- Project stack (Kotlin 2.3.0, Compose MP 1.9.3) is well-positioned

**Deprecated/outdated:**
- Manual tracking of API usage across dependencies
- Hoping Apple won't notice (automated scanning catches everything)
</sota_updates>

<open_questions>
## Open Questions

1. **Firebase SDK Privacy Manifest**
   - What we know: Firebase iOS SDK now ships its own privacy manifest
   - What's unclear: Whether app needs to redeclare APIs also used by Firebase
   - Recommendation: Let Firebase handle its own APIs; only declare app-specific usage

2. **Future API Categories**
   - What we know: Apple considers required reason APIs an "open list"
   - What's unclear: What new APIs might be added
   - Recommendation: Re-run JetBrains finder script before each major submission; monitor Apple news

3. **Compose Multiplatform Future Updates**
   - What we know: JetBrains tracks required reason API usage
   - What's unclear: If future CMP versions will require different declarations
   - Recommendation: Check release notes when upgrading Compose Multiplatform
</open_questions>

<sources>
## Sources

### Primary (HIGH confidence)
- [Privacy manifest files | Apple Developer Documentation](https://developer.apple.com/documentation/bundleresources/privacy-manifest-files) - Official requirements
- [Describing use of required reason API | Apple Developer Documentation](https://developer.apple.com/documentation/bundleresources/describing-use-of-required-reason-api) - API categories and reason codes
- [Privacy manifest for iOS apps | Kotlin Multiplatform Documentation](https://kotlinlang.org/docs/multiplatform/multiplatform-privacy-manifest.html) - KMP-specific guidance
- [TN3183: Adding required reason API entries | Apple Developer Documentation](https://developer.apple.com/documentation/technotes/tn3183-adding-required-reason-api-entries-to-your-privacy-manifest) - Implementation tech note

### Secondary (MEDIUM confidence)
- [Adding a privacy manifest to your app | Apple Developer Documentation](https://developer.apple.com/documentation/bundleresources/adding-a-privacy-manifest-to-your-app-or-third-party-sdk) - Xcode workflow
- [Privacy updates for App Store submissions | Apple Developer](https://developer.apple.com/news/?id=3d8a9yyh) - Timeline and enforcement
- [JetBrains Required Reason Finder](https://github.com/JetBrains/kotlin/blob/rrf_v0.0.1/libraries/tools/required-reason-finder/required_reason_finder.py) - Detection tool

### Tertiary (LOW confidence - needs validation during implementation)
- Community reports on specific Compose Multiplatform API usage patterns
</sources>

<metadata>
## Metadata

**Research scope:**
- Core technology: Apple Privacy Manifest (PrivacyInfo.xcprivacy)
- Ecosystem: Kotlin Multiplatform, Compose Multiplatform, Xcode
- Patterns: Required reason API declaration, file format, target membership
- Pitfalls: Wrong reason codes, missing target membership, old Kotlin versions

**Confidence breakdown:**
- Standard stack: HIGH - Official Apple documentation, well-established since May 2024
- Architecture: HIGH - Apple's mandated file format
- Pitfalls: HIGH - Documented in Apple tech notes and developer forums
- Code examples: HIGH - Verified against official documentation

**Research date:** 2026-01-15
**Valid until:** 2026-07-15 (6 months - stable Apple requirement, watch for new API categories)
</metadata>

---

## Required Reason API Categories Reference

For quick reference, here are all current required reason API categories and their common reason codes:

### NSPrivacyAccessedAPICategoryFileTimestamp
APIs: `stat`, `fstat`, `getattrlist`, etc.
| Code | Use Case |
|------|----------|
| **0A2A.1** | Third-party SDK wrapper (Compose MP) |
| C617.1 | App container files |
| 3B52.1 | User-granted files |
| DDA9.1 | Display to user |

### NSPrivacyAccessedAPICategorySystemBootTime
APIs: `mach_absolute_time`, `systemUptime`
| Code | Use Case |
|------|----------|
| **35F9.1** | Time interval measurement |

### NSPrivacyAccessedAPICategoryUserDefaults
APIs: `NSUserDefaults`
| Code | Use Case |
|------|----------|
| **CA92.1** | App-only access |
| 1C8F.1 | App Group shared |
| C56D.1 | SDK wrapper |

### NSPrivacyAccessedAPICategoryDiskSpace
APIs: `volumeAvailableCapacity`
| Code | Use Case |
|------|----------|
| 85F4.1 | Display to user |
| **E174.1** | Check before write |
| 7D9E.1 | Bug reports |

### NSPrivacyAccessedAPICategoryActiveKeyboards
APIs: `activeInputModes`
| Code | Use Case |
|------|----------|
| Custom keyboard apps only |

**Bold codes** are most commonly needed for Compose Multiplatform apps.

---

*Phase: 01-ios-privacy-manifest*
*Research completed: 2026-01-15*
*Ready for planning: yes*
