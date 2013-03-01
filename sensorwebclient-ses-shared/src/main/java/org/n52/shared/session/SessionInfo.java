
package org.n52.shared.session;

import static java.lang.System.currentTimeMillis;

import java.io.Serializable;
import java.util.Date;

/**
 * 
 */
public class SessionInfo implements Serializable {

    private static final long serialVersionUID = 5076704245184520622L;

    public static final String COOKIE_SESSION_ID = "n52_sensorweb_client_sessionId";

    private Date expiringDate;

    private String username;

    private String userId;

    private String role;

    private String session;

    SessionInfo() {
        // for serialization only
    }

    public SessionInfo(String sessionId) {
        this.expiringDate = createExpiringDate();
        this.session = sessionId;
    }

    private Date createExpiringDate() {
        long oneHour = 1000 * 60 * 60;
        return new Date(currentTimeMillis() + oneHour);
    }

    public boolean isExpired() {
        long delta = expiringDate.getTime() - currentTimeMillis();
        return delta <= 0;
    }

    /**
     * <b>Do not trust items on server side which were sent by the client. Use server's session storage to
     * retrieve accurate infos by checking and using session id.</b>
     * 
     * @return the user's role
     */
    public Date getExpiringDate() {
        return expiringDate;
    }

    /**
     * <b>Do not trust items on server side which were sent by the client. Use server's session storage to
     * retrieve accurate infos by checking and using session id.</b>
     * 
     * @return the user name
     */
    public String getUsername() {
        return username;
    }

    void setUsername(String username) {
        this.username = username;
    }

    /**
     * <b>Do not trust items on server side which were sent by the client. Use server's session storage to
     * retrieve accurate infos by checking and using session id.</b>
     * 
     * @return the user id
     */
    public String getUserId() {
        return userId;
    }

    void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * <b>Do not trust items on server side which were sent by the client. Use server's session storage to
     * retrieve accurate infos by checking and using session id.</b>
     * 
     * @return the user's role
     */
    public String getRole() {
        return role;
    }

    void setRole(String role) {
        this.role = role;
    }

    /**
     * <b>Do not trust items on server side which were sent by the client. Use server's session storage to
     * retrieve accurate infos by checking and using session id.</b>
     * 
     * @return the user's role
     */
    public String getSession() {
        return session;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( (expiringDate == null) ? 0 : expiringDate.hashCode());
        result = prime * result + ( (role == null) ? 0 : role.hashCode());
        result = prime * result + ( (session == null) ? 0 : session.hashCode());
        result = prime * result + ( (userId == null) ? 0 : userId.hashCode());
        result = prime * result + ( (username == null) ? 0 : username.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if ( ! (obj instanceof SessionInfo))
            return false;
        SessionInfo other = (SessionInfo) obj;
        if (expiringDate == null) {
            if (other.expiringDate != null)
                return false;
        }
        else if ( !expiringDate.equals(other.expiringDate))
            return false;
        if (role == null) {
            if (other.role != null)
                return false;
        }
        else if ( !role.equals(other.role))
            return false;
        if (session == null) {
            if (other.session != null)
                return false;
        }
        if (userId == null) {
            if (other.userId != null)
                return false;
        }
        else if ( !userId.equals(other.userId))
            return false;
        if (username == null) {
            if (other.username != null)
                return false;
        }
        else if ( !username.equals(other.username))
            return false;
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("LoginCookie [ ");
        sb.append("user: ").append(username).append(", ");
        sb.append("userId: ").append(userId).append(", ");
        sb.append("role: ").append(role).append(", ");
        sb.append("session: ").append(session);
        return sb.append(" ]").toString();
    }

}
