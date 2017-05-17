var ws;
var client;
var subscription;

$(document).ready(function() {
	initConn();
	checkBox = document.getElementById('history');
	checkBox.checked = false;
	checkBox = document.getElementById('pista');
	checkBox.checked = false;
});

var app = angular
		.module('myApp', [])
		.config(
				function($httpProvider) {
					$httpProvider.defaults.headers.common['X-Requested-With'] = 'XMLHttpRequest';
				});
app.controller('myCtrl', function($location, $scope, $http) {

	// Inicializar los tweets
	$scope.tweets = [];
	$scope.authenticated = false;
	$scope.user = true;

	this.logout = function() {

		$http({
			method : 'POST',
			url : '/logout'
		}).then(function(success) {
			data = success.data
			console.log(data);
			$scope.authenticated = false;
			$location.path("/");
			subscription.unsubscribe(); //no haria falta porque no te llegarían los mensajes por seguridad
		}, function(error) {
			console.log("Logout failed")
			self.authenticated = false;
		});

	};

	// si se pide una busqueda
	this.getTweets = function() {

		$http({
			method : 'GET',
			url : '/user'
		}).then(
				function(success) {
					data = success.data
					if (data == "" || data == {}) {

						console.log("NO autentificado");
						$scope.user = "N/A";
						$scope.authenticated = false;
					} else {

						console.log(data);
						$scope.user = data.userAuthentication.details.name;
						$scope.authenticated = true;
						console.log("autentificado");
						if (subscription != undefined) {
							// si ya estabas subscrito a anteriores busquedas te
							// desuscribes
							// (solo una abierta a la vez)
							subscription.unsubscribe();
							$scope.tweets = [];
						}

						if (document.getElementById('history').checked) {
							console.log("history checked");

							$.get('/search', {
								q : $("#q").val(),
								restriccion : $("#restriccion").val(),
								dificultad : $("#dificultad").val()
							}).done(function(data, status) {
								console.log(data);
								$scope.tweets = data;
								$scope.$apply();
							}).fail(function(data, status) {
								console.log(data);
							});

						} else {
							console.log("history NO checked");

							var query = $("#q").val();
							var dificultad = $("#dificultad").val();
							var restriccion = $("#restriccion").val();
							var hint = "hintNo";
							claveSubscripcion = query + "-" + dificultad + "-"
									+ restriccion;
							if(document.getElementById('pista').checked){
								hint = "hintYes";
							}
							claveSubscripcion = claveSubscripcion + "-" + hint
							//"./upload?${_csrf.parameterName}=${_csrf.token}"
							
							
							var parametros = {
									query : $("#q").val(),
									restriccion : $("#restriccion").val(),
									dificultad : $("#dificultad").val(),
									hint : hint
								}
							
							$http({
								method : 'POST',
								url : '/config',
								params: parametros
							}).then(function(success) {
								data = success.data
								console.log(data);
							}, function(error) {
								console.log("Logout failed")
							});
							
							

							client.send("/app/search", {}, claveSubscripcion);
							// Te subscribes a la busqueda. La funcion se
							// ejecuta
							// cuando el servidor
							// envia algo por la cola subscrita

							console.log(claveSubscripcion);
							subscription = client.subscribe("/queue/search/"
									+ claveSubscripcion, function(mensaje) {
								var data = JSON.parse(mensaje.body);
								$scope.tweets.unshift(data);
								$scope.$apply();
							}, {
								id : query
							});
						}
					}
				}, function(error) {
					console.log("NO autentificado");

					$scope.user = "N/A";
					$scope.authenticated = false;

				});

	};
});

/*
 * Iniciarlizar la conexion con el servidor con el endpoint (del websocket)
 * llamado twitter
 */
function initConn() {
	ws = new SockJS("/twitter");
	client = Stomp.over(ws);
	var headers = {};

	var connect_callback = function() {
		// called back after the client is connected and authenticated to the
		// STOMP server
	};
	var error_callback = function(error) {
		// display the error's message header:
		console.log(error);
	};
	client.connect(headers, connect_callback, error_callback);
}
