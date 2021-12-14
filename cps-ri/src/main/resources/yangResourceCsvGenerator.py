import csv
import hashlib

with open('../../../../cps-service/src/test/resources/dmi-registry.yang') as content:
    lines = content.read()

# open the file in the write mode
with open('changelog/db/changes/data/dmi/generated_yang_resource.csv', 'w', newline='') as file:
    writer = csv.writer(file, delimiter='|')
    writer.writerow(["name", "content", "checksum"])
    writer.writerow(["dmi-registry@2021-12-13.yang", lines,
                     hashlib.sha256().hexdigest()])

# close the file
file.close()