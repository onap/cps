docker run -it --rm -p 8783:8080 -v "$(pwd)"/mappings:/home/wiremock/mappings -v "$(pwd)"/files:/home/wiremock/__files --name wiremock wiremock/wiremock:2.33.2 --verbose

