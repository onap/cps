set -exu

ROBOT3_VENV=$(mktemp -d --suffix=robot_venv)
echo "ROBOT3_VENV=${ROBOT3_VENV}" >> "${WORKSPACE}/env.properties"

echo "Python version is: $(python3 --version)"

python3 -m venv "${ROBOT3_VENV}"
source "${ROBOT3_VENV}/bin/activate"

# Make sure pip3 itself us up-to-date.
python3 -m pip install --upgrade pip

echo "Installing Python Requirements"
python3 -m pip install -r ${WORKSPACE}/pylibs.txt
python3 -m pip freeze
