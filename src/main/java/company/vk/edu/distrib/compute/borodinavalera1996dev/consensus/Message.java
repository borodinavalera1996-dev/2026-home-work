package company.vk.edu.distrib.compute.borodinavalera1996dev.consensus;

public record Message(int idNode, Type type) {
    public enum Type { PING, ELECT, ANSWER, VICTORY}
}
