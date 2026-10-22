# sync_store_listing.rb
#
# Parses STORE_LISTING.md and generates fastlane metadata files
# for both Android (Play Store) and iOS (App Store Connect).
#
# Usage:
#   bundle exec fastlane sync_store_listing
#
# Or from Ruby:
#   require_relative 'sync_store_listing'
#   SyncStoreListing.sync("../store_assets/STORE_LISTING.md", "./fastlane/metadata")

require 'fileutils'

module SyncStoreListing
  # Main entry point - syncs metadata from STORE_LISTING.md
  def self.sync(store_listing_path, output_base_path)
    unless File.exist?(store_listing_path)
      UI.error("Store listing file not found: #{store_listing_path}")
      UI.message("Create your store listing at: #{store_listing_path}")
      return false
    end

    content = File.read(store_listing_path)

    # Check for placeholder content
    if content.include?("[Your app name]") || content.include?("[placeholder]")
      UI.important("STORE_LISTING.md contains placeholder content.")
      UI.message("Please update the file with your actual app content before syncing.")
    end

    # Parse sections from markdown
    sections = parse_markdown(content)

    # Generate metadata for each platform
    generate_android_metadata(sections, output_base_path)
    generate_ios_metadata(sections, output_base_path)

    UI.success("Metadata synced from #{store_listing_path}")
    true
  end

  private

  # Parse the markdown file and extract content sections
  def self.parse_markdown(content)
    sections = {}

    # App Name (from header or explicit field)
    if match = content.match(/\*\*App Name\*\*:\s*(.+)/)
      sections[:app_name] = clean_placeholder(match[1])
    end

    # Google Play Short Description (80 chars)
    if match = content.match(/### Short Description.*?```\s*\n(.*?)\n```/m)
      sections[:short_description] = clean_text(match[1])
    end

    # Google Play Full Description (4000 chars)
    if match = content.match(/### Full Description.*?```\s*\n(.*?)\n```/m)
      sections[:full_description] = clean_text(match[1])
    end

    # Google Play Keywords/Tags
    if match = content.match(/### Keywords\/Tags.*?```\s*\n(.*?)\n```/m)
      sections[:android_keywords] = clean_text(match[1])
    end

    # iOS Subtitle (30 chars)
    if match = content.match(/### Subtitle.*?```\s*\n(.*?)\n```/m)
      sections[:subtitle] = clean_text(match[1])
    end

    # iOS Promotional Text (170 chars)
    if match = content.match(/### Promotional Text.*?```\s*\n(.*?)\n```/m)
      sections[:promotional_text] = clean_text(match[1])
    end

    # iOS Description - look specifically in App Store section
    if match = content.match(/## Apple App Store.*?### Description.*?```\s*\n(.*?)\n```/m)
      sections[:ios_description] = clean_text(match[1])
    end

    # iOS Keywords (100 chars, comma-separated)
    if match = content.match(/### Keywords \(100 characters.*?```\s*\n(.*?)\n```/m)
      sections[:ios_keywords] = clean_text(match[1])
    end

    # What's New / Release Notes
    if match = content.match(/### What's New.*?```\s*\n(.*?)\n```/m)
      sections[:whats_new] = clean_text(match[1])
    end

    # Privacy Policy URL
    if match = content.match(/\*\*Privacy Policy URL\*\*:\s*(.+)/)
      url = clean_placeholder(match[1])
      sections[:privacy_url] = url unless url.include?("example.com")
    end

    # Support URL
    if match = content.match(/\*\*Support URL\*\*:\s*(.+)/)
      url = clean_placeholder(match[1])
      sections[:support_url] = url unless url.include?("example.com")
    end

    # Marketing URL
    if match = content.match(/\*\*Marketing URL\*\*:\s*(.+)/)
      url = clean_placeholder(match[1])
      sections[:marketing_url] = url unless url.include?("example.com")
    end

    sections
  end

  # Clean placeholder text and trim whitespace
  def self.clean_placeholder(text)
    return "" unless text
    text.strip
        .gsub(/\[.*?\]/, '')  # Remove [placeholder] markers
        .gsub(/\(.*?\)/, '')  # Remove (notes) markers
        .strip
  end

  # Clean multiline text content
  def self.clean_text(text)
    return "" unless text
    text.strip
  end

  # Generate Android (Play Store) metadata files
  def self.generate_android_metadata(sections, output_base_path)
    base_path = File.join(output_base_path, "android", "en-US")
    FileUtils.mkdir_p(base_path)
    FileUtils.mkdir_p(File.join(base_path, "changelogs"))

    # Title (app name) - 30 chars max
    write_if_present(
      File.join(base_path, "title.txt"),
      truncate(sections[:app_name], 30)
    )

    # Short description - 80 chars max
    write_if_present(
      File.join(base_path, "short_description.txt"),
      truncate(sections[:short_description], 80)
    )

    # Full description - 4000 chars max
    write_if_present(
      File.join(base_path, "full_description.txt"),
      truncate(sections[:full_description], 4000)
    )

    # Changelog (What's New) - use "default.txt" for versionless changelog
    write_if_present(
      File.join(base_path, "changelogs", "default.txt"),
      truncate(sections[:whats_new], 500)
    )

    UI.message("Generated Android metadata in: #{base_path}")
  end

  # Generate iOS (App Store Connect) metadata files
  def self.generate_ios_metadata(sections, output_base_path)
    base_path = File.join(output_base_path, "ios", "en-US")
    FileUtils.mkdir_p(base_path)

    # App name - 30 chars max
    write_if_present(
      File.join(base_path, "name.txt"),
      truncate(sections[:app_name], 30)
    )

    # Subtitle - 30 chars max
    write_if_present(
      File.join(base_path, "subtitle.txt"),
      truncate(sections[:subtitle], 30)
    )

    # Description - 4000 chars max
    # Use iOS-specific description if available, otherwise fall back to full_description
    description = sections[:ios_description] || sections[:full_description]
    write_if_present(
      File.join(base_path, "description.txt"),
      truncate(description, 4000)
    )

    # Keywords - 100 chars max, comma-separated
    write_if_present(
      File.join(base_path, "keywords.txt"),
      truncate(sections[:ios_keywords], 100)
    )

    # Promotional text - 170 chars max
    write_if_present(
      File.join(base_path, "promotional_text.txt"),
      truncate(sections[:promotional_text], 170)
    )

    # Release notes (What's New) - 4000 chars max
    write_if_present(
      File.join(base_path, "release_notes.txt"),
      truncate(sections[:whats_new], 4000)
    )

    # Privacy URL
    write_if_present(
      File.join(base_path, "privacy_url.txt"),
      sections[:privacy_url]
    )

    # Support URL
    write_if_present(
      File.join(base_path, "support_url.txt"),
      sections[:support_url]
    )

    # Marketing URL
    write_if_present(
      File.join(base_path, "marketing_url.txt"),
      sections[:marketing_url]
    )

    UI.message("Generated iOS metadata in: #{base_path}")
  end

  # Write content to file if present and non-empty
  def self.write_if_present(path, content)
    return unless content && !content.strip.empty?
    File.write(path, content)
    UI.message("  Created: #{File.basename(path)}")
  end

  # Truncate string to max length
  def self.truncate(text, max_length)
    return "" unless text
    return text if text.length <= max_length
    text[0...(max_length - 3)] + "..."
  end
end

# Allow running directly for testing
if __FILE__ == $0
  require 'fastlane'
  SyncStoreListing.sync(
    ARGV[0] || "../store_assets/STORE_LISTING.md",
    ARGV[1] || "./metadata"
  )
end
