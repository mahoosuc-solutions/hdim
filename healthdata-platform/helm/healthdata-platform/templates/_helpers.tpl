{{/*
Expand the name of the chart.
*/}}
{{- define "healthdata-platform.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
*/}}
{{- define "healthdata-platform.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "healthdata-platform.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "healthdata-platform.labels" -}}
helm.sh/chart: {{ include "healthdata-platform.chart" . }}
{{ include "healthdata-platform.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "healthdata-platform.selectorLabels" -}}
app.kubernetes.io/name: {{ include "healthdata-platform.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "healthdata-platform.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "healthdata-platform.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Database connection string
*/}}
{{- define "healthdata-platform.databaseUrl" -}}
jdbc:postgresql://{{ .Values.database.host }}:{{ .Values.database.port }}/{{ .Values.database.name }}
{{- end }}

{{/*
Redis connection string
*/}}
{{- define "healthdata-platform.redisHost" -}}
{{ .Values.redis.host }}
{{- end }}

{{/*
Kafka bootstrap servers
*/}}
{{- define "healthdata-platform.kafkaBootstrapServers" -}}
{{ .Values.kafka.bootstrapServers }}
{{- end }}
