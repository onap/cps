{{- define "cps-and-ncmp.name" -}}
{{ .Chart.Name }}
{{- end }}

{{- define "cps-and-ncmp.fullname" -}}
{{ .Release.Name }}-{{ .Chart.Name }}
{{- end }}
