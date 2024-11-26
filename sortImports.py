import os

def sort_imports(java_file_path):
    with open(java_file_path, 'r', encoding='utf-8') as file:
        lines = file.readlines()

    # Separate license block
    license_block = []
    package_line = None
    static_imports = []
    normal_imports = []
    other_lines = []

    i = 0

    # Process license block
    if lines and lines[0].startswith('/**'):
        for i, line in enumerate(lines):
            license_block.append(line)
            if line.strip().startswith('*/'):
                i += 1  # Move past the license block
                break
    else:
        license_block = []

    # Process remaining lines
    while i < len(lines):
        line = lines[i]
        if line.startswith('package '):
            package_line = line
        elif line.startswith('import static '):
            static_imports.append(line)
        elif line.startswith('import '):
            normal_imports.append(line)
        else:
            other_lines.append(line)
        i += 1

    # Sort imports
    static_imports.sort()
    normal_imports.sort()

    # Build the new file content
    new_content = []

    # Add license block (always at the top)
    if license_block:
        new_content.extend(license_block)
        if not license_block[-1].endswith('\n'):
            new_content.append('\n')

    # Add package line
    if package_line:
        new_content.append(package_line)
        if not package_line.endswith('\n'):
            new_content.append('\n')

    # Add imports
    if static_imports:
        new_content.append('\n')  # Separate static imports
        new_content.extend(static_imports)
    if normal_imports:
        new_content.append('\n')  # Separate normal imports
        new_content.extend(normal_imports)

    # Add remaining lines (code, comments, etc.)
    if other_lines:
        if not new_content[-1].endswith('\n'):
            new_content.append('\n')
        new_content.extend(other_lines)

    # Write back to file
    with open(java_file_path, 'w', encoding='utf-8') as file:
        file.writelines(new_content)

def process_java_files(root_dir):
    for subdir, _, files in os.walk(root_dir):
        for file in files:
            if file.endswith('.java'):
                java_file_path = os.path.join(subdir, file)
                print(f"Processing: {java_file_path}")
                sort_imports(java_file_path)

if __name__ == "__main__":
    project_root = input("Enter the root directory of the Java project: ")
    process_java_files(project_root)

