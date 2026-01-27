{{- define "cps-and-ncmp.name" -}}
{{ .Chart.Name }}
{{- end }}

{{- define "cps-and-ncmp.release-name" -}}
{{ .Release.Name }}-{{ .Chart.Name }}
{{- end }}
