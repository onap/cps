ROBOT_VENV=$(mktemp -d --suffix=robot_venv)
echo "ROBOT_VENV=${ROBOT_VENV}" >> "${WORKSPACE}/env.properties"

echo "Python version is: $(python3 --version)"

python3 -m venv "${ROBOT_VENV}"
source "${ROBOT_VENV}/bin/activate"

set -exu

# Make sure pip3 itself us up-to-date.
python3 -m pip install --upgrade pip

echo "Installing Python Requirements"
python3 -m pip install -r ${WORKSPACE}/pylibs.txt
python3 -m pip freeze
