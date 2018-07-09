# LeilaoSD

Ana Paula Fernandes de Souza<br>
Luiz Eduardo Pereira<br />
Disciplina : Sistema Distribuído<br />
Professor : Everthon Valadão<br />
Faculdade : Instituto Federal de Minas Gerais Campus Formiga(IFMG)<br />
Atualização : 09/07/2018<br />

# Funcionamento:

O serviço de leilão deverá permitir o anúncio de novos itens para serem leiloados, criação de
sala(s) de leilão com os usuários interessados em um mesmo item, bem como receber os lances feitos
pelos usuários. Um determinado item (cadastrado no sistema por um usuário) pode ser leiloado em
apenas uma sala de leilão por vez. Em caso de empate em uma rodada de lances, o leilão do item deverá
continuar até que haja um maior lance. Após passado algum tempo sem novos lances, o lance de maior
valor para um determinado item será considerado como vencedor, passando o item a ser de propriedade
do usuário que enviou o lance vencedor. Observe que NÃO poderá haver dois vencedores de um mesmo
leilão, bem como um mesmo item NÃO poderá ser leiloado "simultaneamente" em dois ou mais leilões.

# Executar : 

Trabalho com o padrão de projeto MVC (Modelo – Visão – Controle):

Visão : compila-e-executaVisao.sh<br />
Controle : compila-e-executaControle.sh<br />
Modelo : compila-e-executa-Persistencia.sh<br />



# Possíveis melhorias no trabalho  : 

1) Criar arquivo do xml para a visao,controle,modelo otimizado para cada estrutura.

2) Quando o leiloeiro(pessoa responsavel pelo leilao), falhar ser substituido por outro.

3) Quando um usuario falhar e retornar, voltar para a sala de leilao que participava.

4) Reorganização do codigo e otimização.
