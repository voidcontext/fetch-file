FROM nginx:stable

COPY ./100MB.bin /var/www/100MB.bin
COPY ./nginx.conf /etc/nginx/conf.d/default.conf

CMD ["nginx", "-g", "daemon off;"]
