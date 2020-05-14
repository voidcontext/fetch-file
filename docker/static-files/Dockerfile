FROM nginx:stable

COPY ./10MB.bin /var/www/10MB.bin
COPY ./10MB.bin.gz /var/www/10MB.bin.gz
COPY ./100MB.bin /var/www/100MB.bin
COPY ./100MB.bin.gz /var/www/100MB.bin.gz
COPY ./nginx.conf /etc/nginx/conf.d/default.conf

CMD ["nginx", "-g", "daemon off;"]
