/**
 * Created by manas on 22-09-2015.
 */

module.exports = {
    user1_sample_data: {
        name: "name1",
        email: "testemail1@test.com",
        password: "123456_",
        createdAt: 5000
    },
    user2_sample_data: {
        name: "name2",
        email: "testemail2@test.com",
        password: "123456_",
        createdAt: 5000
    },
    createUser: function(api, sampleData, callback) {
        api.post("/api/users")
            .send({
                name: sampleData.name,
                email: sampleData.email,
                password: sampleData.password
            })
            .end(function (err, res) {
                if (err) return callback(err);

                return callback(err, res);
            });
    }
};