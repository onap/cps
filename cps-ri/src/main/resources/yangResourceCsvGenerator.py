import csv
import hashlib
import sys

yang_source = ''
checksum = ''

for yang_source in sys.argv[1:]:
    checksum = hashlib.sha256(str(yang_source).encode()).hexdigest()

with open('changelog/db/changes/data/yang-models/' + yang_source + '.yang') as content:
    dmiRegistry = content.read()

# open the file in the write mode
with open('changelog/db/changes/data/dmi/generated_yang_resource_' + yang_source + '.csv', 'w', newline='') \
        as file:
    writer = csv.writer(file, delimiter='|')
    writer.writerow(["name", "content", "checksum"])
    writer.writerow([yang_source + '.yang', dmiRegistry, checksum])
