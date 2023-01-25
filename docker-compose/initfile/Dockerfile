# syntax=docker/dockerfile:1
FROM alpine:latest
RUN apk --no-cache add curl
RUN apk --no-cache add bash
COPY onboardSubscriptionModel.sh /onboardSubscriptionModel.sh
CMD ["sh", "onboardSubscriptionModel.sh"]